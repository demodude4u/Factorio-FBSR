package com.demod.fbsr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.rapidoid.commons.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModLoader;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.cli.CmdBot;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUIStyle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Profile {
	private static final Logger LOGGER = LoggerFactory.getLogger(Profile.class);

    public static final Set<String> BUILTIN_MODS;
	public static final Set<String> BASE_ENTITIES;
	public static final Set<String> BASE_TILES;
	public static final Map<String, String> RENDERING_MAP;

	static {
		JSONObject json;
		try (InputStream is = Profile.class.getClassLoader().getResourceAsStream("base-data.json")) {
			json = new JSONObject(new JSONTokener(is));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load base-data.json", e);
		}
        BUILTIN_MODS = json.getJSONArray("mods").toList().stream()
                .map(Object::toString).collect(Collectors.toSet());
		BASE_ENTITIES = json.getJSONArray("entities").toList().stream()
				.map(Object::toString).collect(Collectors.toSet());
		BASE_TILES = json.getJSONArray("tiles").toList().stream()
				.map(Object::toString).collect(Collectors.toSet());
		RENDERING_MAP = json.getJSONObject("rendering").toMap().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
	}

    //The BUILD statuses are named after the next step in the build process
    public static enum ProfileStatus {
        INVALID,
        DISABLED,
        BUILD_1_MANIFEST, // Config Updates
        BUILD_2_DOWNLOAD, // Mod Updates
        BUILD_3_DUMP, // Factorio Updates
        BUILD_4_DATA, // Rendering Updates
        READY,
    }

    private final String name;
    
    private final File folderProfile;
    private final File fileProfile;
    private final File fileFactorioData;
    private final File fileAtlasData;
    
    private final File folderBuild;
    private final File fileManifest;

    private final File folderBuildMods;
    private final File fileModList;

    private final File folderBuildData;
    private final File fileScriptOutputDump;

    private volatile FactorioData factorioData = null;
    private volatile AtlasPackage atlasPackage = null;

    public static Profile byName(String name) {
        JSONObject json = Config.get().getJSONObject("factorio_manager");
        File folderProfileRoot = new File(json.optString("profiles", "profiles"));
        File folderBuildRoot = new File(json.optString("build", "build"));

        return new Profile(new File(folderProfileRoot, name), new File(folderBuildRoot, name));
    }

    public static Profile vanilla() {
        return Profile.byName("vanilla");
    }

    public Profile(File folderProfile, File folderBuild) {
        name = folderProfile.getName();

        this.folderProfile = folderProfile;
        this.folderBuild = folderBuild;

        fileProfile = new File(folderProfile, "profile.json");
        fileFactorioData = new File(folderProfile, "factorio-data.zip");
        fileAtlasData = new File(folderProfile, "atlas-data.zip");
        
        fileManifest = new File(folderBuild, "manifest.json");

        folderBuildMods = new File(folderBuild, "mods");
        fileModList = new File(folderBuildMods, "mod-list.json");

        folderBuildData = new File(folderBuild, "data");
        File folderScriptOutput = new File(folderBuildData, "script-output");
        fileScriptOutputDump = new File(folderScriptOutput, "data-raw-dump.zip");

        factorioData = new FactorioData(fileFactorioData);
        atlasPackage = new AtlasPackage(fileAtlasData);
    }

    public String getName() {
        return name;
    }

    public File getFolderProfile() {
        return folderProfile;
    }

    public File getFolderBuild() {
        return folderBuild;
    }

    public File getFileProfile() {
        return fileProfile;
    }

    public File getFileAtlasData() {
        return fileAtlasData;
    }

    public File getFileFactorioData() {
        return fileFactorioData;
    }

    public FactorioData getFactorioData() {
        return factorioData;
    }

    public AtlasPackage getAtlasPackage() {
        return atlasPackage;
    }

    public static List<Profile> listProfiles() {
        JSONObject json = Config.get().getJSONObject("factorio_manager");
        File folderProfileRoot = new File(json.optString("profiles", "profiles"));
        File folderBuildRoot = new File(json.optString("build", "build"));

        List<Profile> profiles = new ArrayList<>();
        for (File folderProfile : folderProfileRoot.listFiles()) {
            Profile profile = new Profile(folderProfile, new File(folderBuildRoot, folderProfile.getName()));
            profiles.add(profile);
        }
        return profiles;
    }

    public boolean isValid() {
        return fileProfile.exists();
    }

    public boolean isEnabled() {
        if (!isValid()) {
            return false;
        }

        JSONObject jsonProfile = readJsonFile(fileProfile);
        return jsonProfile.optBoolean("enabled", true);
    }

    public boolean hasManifest() {
        return fileManifest.exists();
    }

    public boolean hasDownloaded() {
        if (!hasManifest()) {
            return false;
        }

        if (!fileModList.exists()) {
            return false;
        }

        JSONObject jsonManifest = readJsonFile(fileManifest);
        JSONObject jsonZips = jsonManifest.optJSONObject("zips");
        if (jsonZips == null) {
            return false;
        }
        for (String zipName : jsonZips.keySet()) {
            File zipFile = new File(folderBuildMods, zipName);
            if (!zipFile.exists()) {
                return false;
            }
        }

        return true;
    }

    public boolean hasDump() {
        return fileScriptOutputDump.exists();
    }

    public boolean hasData() {
        return fileFactorioData.exists() && fileAtlasData.exists();
    }

    public ProfileStatus getStatus() {
        if (!isValid()) {
            return ProfileStatus.INVALID;
        } else if (!isEnabled()) {
            return ProfileStatus.DISABLED;
        } else if (!hasManifest()) {
            return ProfileStatus.BUILD_1_MANIFEST;
        } else if (!hasDownloaded()) {
            return ProfileStatus.BUILD_2_DOWNLOAD;
        } else if (!hasDump()) {
            return ProfileStatus.BUILD_3_DUMP;
        } else if (!hasData()) {
            return ProfileStatus.BUILD_4_DATA;
        }
        return ProfileStatus.READY;
    }

    public boolean setEnabled(boolean enabled) {
        if (!isValid()) {
            return false;
        }

        JSONObject jsonProfile = readJsonFile(fileProfile);
        jsonProfile.put("enabled", enabled);
        writeJsonFile(fileProfile, jsonProfile);
        return true;
    }

    public boolean generateProfile(String... mods) {
        if (isValid()) {
            System.out.println("Profile " + folderProfile.getName() + " already exists.");
            return false;
        }
        
        JSONObject jsonProfile = readJsonFile(fileProfile);
        jsonProfile.put("enabled", true);
        JSONArray jsonMods = new JSONArray();
        for (String mod : mods) {
            jsonMods.put(mod);
        }
        jsonProfile.put("mods", jsonMods);
        writeJsonFile(fileProfile, jsonProfile);
        System.out.println("Profile created: " + folderProfile.getAbsolutePath());
        return true;
    }

    public static boolean generateDefaultVanillaProfile() {
        Profile editor = Profile.vanilla();
        if (editor.isValid()) {
            System.out.println("Default vanilla profile already exists.");
            return false;
        }
        
        File folderProfile = editor.getFolderProfile();
        folderProfile.mkdirs();
        try (InputStream is = Profile.class.getClassLoader().getResourceAsStream("profile.json")) {
            Files.copy(is, new File(folderProfile, "profile.json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean generateDefaultRenderingConfiguration() {
        if (!isValid() || !hasDump()) {
            return false;
        }

        JSONObject jsonProfile = readJsonFile(fileProfile);
        
        FactorioData factorioData = new FactorioData(fileFactorioData);
        try {
            factorioData.initialize(false);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
        DataTable table = factorioData.getTable();

        //Lazy answer to finality problems with lambda expressions
        AtomicBoolean changed = new AtomicBoolean(false);

        String modGroup = folderProfile.getName();

        JSONObject jsonEntities;
        if (jsonProfile.has("entities")) {
            jsonEntities = jsonProfile.getJSONObject("entities");
        } else {
            jsonEntities = new JSONObject();
            jsonProfile.put("entities", jsonEntities);
            changed.set(true);
        }
        JSONObject jsonEntitiesModGroup;
        if (jsonEntities.has(modGroup)) {
            jsonEntitiesModGroup = jsonEntities.getJSONObject(modGroup);
        } else {
            jsonEntitiesModGroup = new JSONObject();
            jsonEntities.put(modGroup, jsonEntitiesModGroup);
            changed.set(true);
        }
        table.getEntities().values().stream().filter(e -> isBlueprintable(e))
				.sorted(Comparator.comparing(e -> e.getName())).forEach(e -> {
					if (!BASE_ENTITIES.contains(e.getName())) {
						String type = e.getType();
						StringBuilder sb = new StringBuilder();
						for (String part : type.split("-")) {
							sb.append(part.substring(0, 1).toUpperCase() + part.substring(1));
						}
						sb.append("Rendering");
						String rendering = sb.toString();
						if (RENDERING_MAP.containsKey(rendering)) {
							rendering = RENDERING_MAP.get(rendering);
						}
                        if (!jsonEntitiesModGroup.has(e.getName())) {
                            System.out.println("MOD ENTITY ADDED - " + e.getName() + ": " + rendering);
                            jsonEntitiesModGroup.put(e.getName(), rendering);
                            changed.set(true);
                        }
					}
				});

        JSONObject jsonTiles;
        if (jsonProfile.has("tiles")) {
            jsonTiles = jsonProfile.getJSONObject("tiles");
        } else {
            jsonTiles = new JSONObject();
            jsonProfile.put("tiles", jsonTiles);
            changed.set(true);
        }
        JSONArray jsonTilesModGroup;
        if (jsonTiles.has(modGroup)) {
            jsonTilesModGroup = jsonTiles.getJSONArray(modGroup);
        } else {
            jsonTilesModGroup = new JSONArray();
            jsonTiles.put(modGroup, jsonTilesModGroup);
            changed.set(true);
        }
        table.getTiles().values().stream().filter(t -> isBlueprintable(t))
				.sorted(Comparator.comparing(t -> t.getName())).forEach(t -> {
					if (!BASE_TILES.contains(t.getName())) {
                        if (!jsonTilesModGroup.toList().contains(t.getName())) {
                            System.out.println("MOD TILE ADDED - " + t.getName());
                            jsonTilesModGroup.put(t.getName());
                            changed.set(true);
                        }
					}
				});

        if (changed.get()) {
            writeJsonFile(fileProfile, jsonProfile);
            System.out.println("Profile Saved: " + fileProfile.getAbsolutePath());
        }
        return true;
    }

    // based on cannot_ghost() by _codegreen
	private static boolean isBlueprintable(EntityPrototype entity) {
		boolean cantGhost = false;
		if (entity.getFlags().contains("not-blueprintable")) {
			cantGhost = true;
		}
		if (!entity.getFlags().contains("player-creation")) {
			cantGhost = true;
		}
		if (entity.getPlacedBy().isEmpty()) {
			cantGhost = true;
		}
		return !cantGhost;
	}

	private static boolean isBlueprintable(TilePrototype tile) {
		if (!tile.lua().get("can_be_part_of_blueprint").optboolean(true)) {
			return false;
		}
		if (tile.getPlacedBy().isEmpty()) {
			return false;
		}
		return true;
	}

    public boolean clearManifest() {
        return fileManifest.delete();
    }

    public boolean clearAllDownloads() {
        if (!folderBuildMods.exists()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a build folder.");
            return false;
        }
         
        for (File file : folderBuildMods.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                file.delete();
            }
        }

        if (fileModList.exists()) {
            fileModList.delete();
        }

        return true;
    }

    public boolean clearInvalidDownloads() {
        if (!hasManifest()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a manifest.");
            return false;
        }

        JSONObject jsonManifest = readJsonFile(fileManifest);
        JSONObject jsonZips = jsonManifest.optJSONObject("zips");
        if (jsonZips == null) {
            System.out.println("Invalid manifest for profile: " + folderProfile.getName());
            return false;
        }

        Set<String> validZips = jsonZips.keySet();
        if (folderBuildMods.exists()) {
            for (File file : folderBuildMods.listFiles()) {
                if (!validZips.contains(file.getName())) {
                    System.out.println("Deleting invalid download: " + file.getName());
                    file.delete();
                }
            }
        }

        return true;
    }

    public boolean clearDump() {
        return fileScriptOutputDump.delete();
    }

    public boolean clearData() {
        return fileFactorioData.delete() && fileAtlasData.delete();
    }

    public boolean delete() {
        AtomicBoolean success = new AtomicBoolean(true);

        // Delete all files and subdirectories in the build folder
        if (folderBuild.exists()) {
            try {
            Files.walk(folderBuild.toPath())
                 .map(Path::toFile)
                 .sorted((f1, f2) -> f2.getAbsolutePath().length() - f1.getAbsolutePath().length()) // Delete children first
                 .forEach(file -> {
                 if (!file.delete()) {
                     success.set(false);
                 }
                 });
            } catch (IOException e) {
            e.printStackTrace();
            success.set(false);
            }

            if (!folderBuild.delete()) {
                success.set(false);
            }
        }

        // Delete all files and subdirectories in the profile folder
        if (folderProfile.exists()) {
            try {
            Files.walk(folderProfile.toPath())
                 .map(Path::toFile)
                 .sorted((f1, f2) -> f2.getAbsolutePath().length() - f1.getAbsolutePath().length()) // Delete children first
                 .forEach(file -> {
                 if (!file.delete()) {
                     success.set(false);
                 }
                 });
            } catch (IOException e) {
            e.printStackTrace();
            success.set(false);
            }

            if (!folderProfile.delete()) {
                success.set(false);
            }
        }

        return success.get();
    }

    public boolean buildManifest(boolean force) {
        if (!isValid()) {
            System.out.println("Profile " + folderProfile.getName() + " is not valid.");
            return false;
        }

        if (!force && hasManifest()) {
            System.out.println("Profile " + folderProfile.getName() + " already has a manifest.");
            return false;
        }
        
        //TODO load specific versions of mods and calculate dependencies with version compatibility

        JSONObject jsonProfile = readJsonFile(fileProfile);
        JSONArray jsonMods = jsonProfile.optJSONArray("mods");

        List<String> mods = new ArrayList<>();
        for (int i = 0; i < jsonMods.length(); i++) {
            String mod = jsonMods.getString(i).trim();
            mods.add(mod);
        }

        Map<String, JSONObject> modInfo = new LinkedHashMap<>();
        for (String mod : mods) {
            if (BUILTIN_MODS.contains(mod)) {
                continue;
            }
            try {
                walkDependencies(modInfo, mod);
            } catch (IOException e) {
                System.err.println("Failed to walk dependencies for mod: " + mod);
                e.printStackTrace();
                return false;
            }
        }

        JSONObject jsonManifest = new JSONObject();

        JSONObject jsonZips = new JSONObject();
        jsonManifest.put("zips", jsonZips);
        for (Entry<String, JSONObject> entry : modInfo.entrySet()) {
            String modName = entry.getKey();
            JSONObject jsonRelease = entry.getValue();

            String filename = jsonRelease.getString("file_name");
            String downloadUrl = jsonRelease.getString("download_url");
            String sha1 = jsonRelease.getString("sha1");

            JSONArray jsonZip = new JSONArray();
            jsonZip.put(downloadUrl);
            jsonZip.put(sha1);
            jsonZips.put(filename, jsonZip);

            System.out.println("MOD MANIFEST ZIP - [" + modName + "] " + filename + " " + downloadUrl + " " + sha1);
        
            if (!filename.endsWith(".zip")) {
                System.err.println("Invalid mod file name: " + filename + ". Must end with .zip");
                return false;
            }
        }

        if (!folderBuild.exists()) {
            folderBuild.mkdirs();
        }

        writeJsonFile(fileManifest, jsonManifest);

        clearInvalidDownloads();
        clearDump();
        clearData();

        return true;
    }

    private static void walkDependencies(Map<String, JSONObject> results, String mod) throws IOException {
		JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfoFull(mod);
        if (results.put(mod, jsonRelease) != null) {
            return; // Already processed this mod
        }
		JSONArray jsonDependencies = jsonRelease.getJSONObject("info_json").getJSONArray("dependencies");

		for (int i = 0; i < jsonDependencies.length(); i++) {
			String dependency = jsonDependencies.getString(i).trim();
			String prefix = "";
			String depMod = null;

            if (dependency.startsWith("!") || dependency.startsWith("?") || dependency.startsWith("(?)")) {
                continue; // Skip incompatible mods or optional dependencies
            }

			// Extract prefix if present
			if (dependency.startsWith("~")) {
				int prefixEnd = dependency.indexOf(' ');
				prefix = dependency.substring(0, prefixEnd).trim();
				dependency = dependency.substring(prefixEnd).trim();
			}

			// Identify version operator and split dependency
			int versionIndex = -1;
			for (String operator : new String[]{"<=", ">=", "=", "<", ">"}) {
				versionIndex = dependency.indexOf(operator);
				if (versionIndex != -1) {
					break;
				}
			}

			if (versionIndex != -1) {
				depMod = dependency.substring(0, versionIndex).trim();
			} else {
				depMod = dependency; // No version operator found, treat as mod name
			}

			if (!BUILTIN_MODS.contains(depMod)) {
				walkDependencies(results, depMod);
			}
		}
	}

    public boolean buildDownload(boolean force) {

        if (!hasManifest()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a manifest.");
            return false;
        }

        if (!FactorioManager.hasModPortalApi()) {
            System.out.println("Mod Portal API is not configured. Cannot download mods.");
            return false;
        }

        String username = FactorioManager.getModPortalApiUsername();
        String password = FactorioManager.getModPortalApiPassword();

        String authString = null;
        
        JSONObject jsonManifest = readJsonFile(fileManifest);
        JSONObject jsonZips = jsonManifest.optJSONObject("zips");
        if (jsonZips != null) {
            for (String key : jsonZips.keySet()) {
                JSONArray jsonZip = jsonZips.getJSONArray(key);
                String downloadUrl = jsonZip.optString(0);
                String sha1 = jsonZip.optString(1);
                
                File target = new File(folderBuildMods, key);
                if (force || !target.exists()) {

                    try {
                        if (authString == null) {
                            authString = FactorioModPortal.getAuthParams(username, password);
                        }

                        FactorioModPortal.downloadModDirect(target, downloadUrl, sha1, authString);

                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }

        //TODO add mod names to manifest and create a mod-list.json that matches

        clearInvalidDownloads();
        clearDump();
        clearData();

        return true;
    }

    public boolean buildDump(boolean force) {

        if (!hasDownloaded()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have the correct mods downloaded, or the mod list is missing.");
            return false;
        }

        if (!FactorioManager.hasFactorioInstall()) {
            System.out.println("Factorio is not configured. Cannot generate dump file.");
            return false;
        }

        folderBuildData.mkdirs();

        if (force && fileScriptOutputDump.exists()) {
            if (!fileScriptOutputDump.delete()) {
                System.out.println("Failed to delete old dump file: " + fileScriptOutputDump.getAbsolutePath());
                return false;
            }
        }

        Optional<File> folderFactorio = Optional.of(FactorioManager.getFactorioInstall());
        Optional<File> factorioExecutable = Optional.of(FactorioManager.getFactorioExecutable());

        FactorioData data = new FactorioData(folderBuildData, folderBuildMods, folderFactorio, factorioExecutable, force);
        try {
            data.initialize(false);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!hasDump()) {
            System.out.println("Profile " + folderProfile.getName() + " was unable to generate a dump file!");
            return false;
        }

        clearData();

        return true;
    }

    public boolean buildData(boolean force) {
        
        if (!hasDump()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a dump file.");
            return false;
        }

        if (!force && hasData()) {
            System.out.println("Profile " + folderProfile.getName() + " already has data files.");
            return false;
        }

        //TODO

        if (name.equals("vanilla")) {
            if (!GUIStyle.copyFontsToProfile(this)) {
                System.out.println("Failed to copy fonts to vanilla profile.");
                return false;
            }
        }
    }

    public boolean runFactorio() {

        if (!hasDownloaded()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have the correct mods downloaded, or the mod list is missing.");
            return false;
        }
        
        JSONObject json = Config.get().getJSONObject("factorio_manager");

        try {
            JSONObject fdConfig = new JSONObject();
            folderBuildData.mkdirs();

            File folderFactorio = new File(json.getString("install"));
		    File factorioExecutable = new File(json.getString("executable"));

            File fileConfig = new File(folderBuildData, "config.ini");
            try (PrintWriter pw = new PrintWriter(fileConfig)) {
                pw.println("[path]");
                pw.println("read-data=" + folderFactorio.getAbsolutePath());
                pw.println("write-data=" + folderBuildData.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(factorioExecutable.getAbsolutePath(), "--config",
					fileConfig.getAbsolutePath(), "--mod-directory", folderBuildMods.getAbsolutePath());
			pb.directory(folderFactorio);

			LOGGER.debug("Running command " + pb.command().stream().collect(Collectors.joining(",", "[", "]")));

			Process process = pb.start();

			// Create separate threads to handle the output streams
			ExecutorService executor = Executors.newFixedThreadPool(2);
			executor.submit(() -> streamLogging(process.getInputStream(), false));
			executor.submit(() -> streamLogging(process.getErrorStream(), true));
			executor.shutdown();

			// Wait for Factorio to finish
			process.waitFor();
			
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static void streamLogging(InputStream inputStream, boolean error) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (error) {
					LOGGER.error(line);
				} else {
					LOGGER.debug(line);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public List<String> listMods() {


        File fileModList = new File(folderMods, "mod-list.json");
        if (!fileModList.exists()) {
            LOGGER.warn("Mod list file does not exist: {}", fileModList.getAbsolutePath());
            return ImmutableList.of();
        }

        try {
            JSONObject json = new JSONObject(Files.readString(fileModList.toPath()));
        
            JSONArray jsonMods = json.getJSONArray("mods");
            List<String> mods = new ArrayList<>();
            for (int i = 0; i < jsonMods.length(); i++) {
                JSONObject jsonMod = jsonMods.getJSONObject(i);
                String modName = jsonMod.getString("name");
                boolean enabled = jsonMod.optBoolean("enabled", true);
                if (enabled) {
                    mods.add(modName);
                }
            }
            Collections.sort(mods);
            return mods;
        
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return ImmutableList.of();
        }
    }

    public boolean testProfile(String[] mods) {
        try {
            if (!tempDump(mods)) {
                LOGGER.error("Failed to dump profile data.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static JSONObject readJsonFile(File file) {
        try {
            return new JSONObject(Files.readString(file.toPath()));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void writeJsonFile(File file, JSONObject json) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(json.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
