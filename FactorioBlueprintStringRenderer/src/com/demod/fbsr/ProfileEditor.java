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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ProfileEditor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEditor.class);

    public static final Set<String> BUILTIN_MODS;
	public static final Set<String> BASE_ENTITIES;
	public static final Set<String> BASE_TILES;
	public static final Map<String, String> RENDERING_MAP;

	static {
		JSONObject json;
		try (InputStream is = ProfileEditor.class.getClassLoader().getResourceAsStream("base-data.json")) {
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
    
    private final File folderProfile;
    private final File fileProfile;
    private final File fileFactorioData;
    private final File fileAtlasData;
    
    private final File folderBuild;
    private final File fileManifest;

    private final File folderBuildMods;
    private final File fileModList;

    private final File folderBuildData;
    private final File fileScriptOutputDumpZip;

    public ProfileEditor(File folderProfile, File folderBuild) {
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
        fileScriptOutputDumpZip = new File(folderScriptOutput, "data-raw-dump.zip");
    }

    public File getFolderProfile() {
        return folderProfile;
    }

    public File getFolderBuild() {
        return folderBuild;
    }

    public static List<ProfileEditor> listProfiles() {
        JSONObject json = Config.get().getJSONObject("factorio_manager");
        File folderProfileRoot = new File(json.optString("profiles", "profiles"));
        File folderBuildRoot = new File(json.optString("build", "build"));

        List<ProfileEditor> profiles = new ArrayList<>();
        for (File folderProfile : folderProfileRoot.listFiles()) {
            ProfileEditor profile = new ProfileEditor(folderProfile, new File(folderBuildRoot, folderProfile.getName()));
            if (profile.isValid()) {
                profiles.add(profile);
            }
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

        JSONObject jsonManifest = readJsonFile(fileManifest);
        JSONArray jsonZips = jsonManifest.optJSONArray("zips");
        for (int i = 0; i < jsonZips.length(); i++) {
            String zipName = jsonZips.getString(i);
            File zipFile = new File(folderBuildMods, zipName);
            if (!zipFile.exists()) {
                return false;
            }
        }

        return true;
    }

    public boolean hasDump() {
        return fileScriptOutputDumpZip.exists();
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

    public boolean buildManifest(boolean force) {

    }

    public boolean buildDownload(boolean force) {

    }

    public boolean buildDump(boolean force) {

    }

    public boolean buildData(boolean force) {
        
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

    //TODO how do I gracefully handle generating the default rendering configuration, as well as when adding more mods to a profile?

    public static boolean generateProfile(boolean force, String... mods) {

        if (!force && isValid()) {
            System.out.println("Profile " + folderProfile.getName() + " already exists.");
            return false;
        }

        try {
            if (!tempDump(mods)) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        JSONObject jsonModRendering = new JSONObject();
        JSONObject jsonModRenderingEntities = new JSONObject();
        jsonModRendering.put("entities", jsonModRenderingEntities);
        JSONObject jsonModRenderingEntitiesModGroup = new JSONObject();
        jsonModRenderingEntities.put(modGroup, jsonModRenderingEntitiesModGroup);
        for (Map.Entry<String, String> entry : tempModEntityRenderings.entrySet()) {
            jsonModRenderingEntitiesModGroup.put(entry.getKey(), entry.getValue());
        }
        JSONObject jsonModRenderingTiles = new JSONObject();
        jsonModRendering.put("tiles", jsonModRenderingTiles);
        JSONArray jsonModRenderingTilesModGroup = new JSONArray();
        jsonModRenderingTiles.put(modGroup, jsonModRenderingTilesModGroup);
        for (String tile : tempModTiles) {
            jsonModRenderingTilesModGroup.put(tile);
        }

        File fileTempModRendering = new File(tempFolderMods, "mod-rendering.json");
		if (fileTempModRendering.exists()) {
			fileTempModRendering.setWritable(true);
		}
		try (FileWriter fw = new FileWriter(fileTempModRendering)) {
			jsonModRendering.write(fw);
		} catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        File folderMods = new File(folderModsRoot, name);
        File fileModDownload = new File(folderMods, tempFileModDownload.getName());
        File fileModList = new File(folderMods, tempFileModList.getName());
        File fileModRendering = new File(folderMods, fileTempModRendering.getName());
        
        try {
            folderMods.mkdirs();
            Files.copy(tempFileModDownload.toPath(), fileModDownload.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(tempFileModList.toPath(), fileModList.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(fileTempModRendering.toPath(), fileModRendering.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();

            fileModDownload.delete();
            fileModList.delete();
            fileModRendering.delete();
            folderMods.delete();
            return false;
        }

        this.folderMods = folderMods;
        return true;
    }

    private boolean tempDump(String[] mods) throws Exception {
        JSONObject cfgFactorioManager = Config.get().getJSONObject("factorio_manager");

        tempFolder = new File(cfgFactorioManager.optString("temp", "temp"));
        tempFolder.mkdirs();
        tempFolderMods = new File(tempFolder, "mods");
        tempFolderData = new File(tempFolder, "data");
        tempFolderMods.mkdir();
        tempFolderData.mkdir();

        Set<String> allMods = new LinkedHashSet<>();
        Collections.addAll(allMods, mods);
		
        for (String mod : mods) {
            if (BUILTIN_MODS.contains(mod)) {
                continue;
            }
            walkDependencies(allMods, mod);
        }

        LOGGER.info("Requested Mods: " + Arrays.asList(mods).stream().collect(Collectors.joining(", ")));
		LOGGER.info("");
		LOGGER.info("Mod List: " + allMods.stream().collect(Collectors.joining(", ")));
		LOGGER.info("");

        JSONObject jsonModDownload = new JSONObject();
        for (String mod : allMods) {
            if (BUILTIN_MODS.contains(mod)) {
                continue;
            }
            JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfo(mod);
            String version = jsonRelease.getString("version");
            jsonModDownload.put(mod, version);
            LOGGER.info("MOD DOWNLOAD - {}: {}", mod, version);
        }

        JSONObject jsonModList = new JSONObject();
		JSONArray jsonModListMods = new JSONArray();
		jsonModList.put("mods", jsonModListMods);
		for (String mod : allMods) {
			JSONObject jsonMod = new JSONObject();
			jsonMod.put("name", mod);
			jsonMod.put("enabled", true);
			jsonModListMods.put(jsonMod);
            LOGGER.info("MOD LIST - {}", mod);
		}
		for (String mod : BUILTIN_MODS) {
			if (allMods.contains(mod)) {
				continue;
			}
			JSONObject jsonMod = new JSONObject();
			jsonMod.put("name", mod);
			jsonMod.put("enabled", false);
			jsonModListMods.put(jsonMod);
            LOGGER.info("MOD LIST - {}", mod);
		}

        tempFileModDownload = new File(tempFolderMods, "mod-download.json");
		if (tempFileModDownload.exists()) {
			tempFileModDownload.setWritable(true);
		}
		try (FileWriter fw = new FileWriter(tempFileModDownload)) {
			jsonModDownload.write(fw);
		}

        FactorioManager.downloadMods(tempFolderMods);

        tempFileModList = new File(tempFolderMods, "mod-list.json");
		if (tempFileModList.exists()) {
			tempFileModList.setWritable(true);
		}
		try (FileWriter fw = new FileWriter(tempFileModList)) {
			jsonModList.write(fw);
		}

        JSONObject fdConfig = new JSONObject();
        tempFolderData.mkdir();
        fdConfig.put("mods", tempFolderMods.getAbsolutePath());
        fdConfig.put("data", tempFolderData.getAbsolutePath());

        String factorioInstall = cfgFactorioManager.getString("install");
		String factorioExecutable = cfgFactorioManager.getString("executable");
        
        JSONObject cfgFactorioData = new JSONObject();
		cfgFactorioData.put("factorio", factorioInstall);
		cfgFactorioData.put("executable", factorioExecutable);
		cfgFactorioData.put("data", tempFolderData.getAbsolutePath());
		cfgFactorioData.put("mods", tempFolderMods.getAbsolutePath());
		FactorioData factorioData = new FactorioData(cfgFactorioData);
		
         if (!factorioData.initialize(false)) {
            LOGGER.error("Failed to initialize Factorio data.");
            return false;
         }

		tempTable = factorioData.getTable();

        tempModEntityRenderings = new LinkedHashMap<String, String>();
		tempTable.getEntities().values().stream().filter(e -> isBlueprintable(e))
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
						tempModEntityRenderings.put(e.getName(), rendering);
                        LOGGER.info("MOD ENTITY - {}: {}", e.getName(), rendering);
					}
				});

        tempModTiles = new ArrayList<>();
		tempTable.getTiles().values().stream().filter(t -> isBlueprintable(t))
				.sorted(Comparator.comparing(t -> t.getName())).forEach(t -> {
					if (!BASE_TILES.contains(t.getName())) {
                        tempModTiles.add(t.getName());
                        LOGGER.info("MOD TILE - {}", t.getName());
					}
				});

        return true;
    }

    private static void walkDependencies(Set<String> allMods, String mod) throws IOException {
		JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfoFull(mod);
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

			// Add the dependency and recursively walk its dependencies
			if (allMods.add(depMod) && !BUILTIN_MODS.contains(depMod)) {
				walkDependencies(allMods, depMod);
			}
		}
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

    public void runFactorio() {
        
        JSONObject json = Config.get().getJSONObject("factorio_manager");

		File folderDataRoot = new File(json.optString("data", "data"));
		folderDataRoot.mkdirs();

        try {
            FactorioManager.downloadMods(folderMods);
        
            JSONObject fdConfig = new JSONObject();
            File folderData = new File(folderDataRoot, folderMods.getName());
            folderData.mkdirs();

            File folderFactorio = new File(json.getString("install"));
		    File factorioExecutable = new File(json.getString("executable"));

            File fileConfig = new File(folderData, "config.ini");
            try (PrintWriter pw = new PrintWriter(fileConfig)) {
                pw.println("[path]");
                pw.println("read-data=" + folderFactorio.getAbsolutePath());
                pw.println("write-data=" + folderData.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(factorioExecutable.getAbsolutePath(), "--config",
					fileConfig.getAbsolutePath(), "--mod-directory", folderMods.getAbsolutePath());
			pb.directory(folderFactorio);

			LOGGER.debug("Running command " + pb.command().stream().collect(Collectors.joining(",", "[", "]")));

			Process process = pb.start();

			// Create separate threads to handle the output streams
			ExecutorService executor = Executors.newFixedThreadPool(2);
			executor.submit(() -> streamLogging(process.getInputStream(), false));
			executor.submit(() -> streamLogging(process.getErrorStream(), true));
			executor.shutdown();

			// Wait for Factorio to finish
			boolean finished = process.waitFor(1, TimeUnit.MINUTES);
			if (!finished) {
				LOGGER.error("Factorio did not exit!");
				process.destroyForcibly();
				process.onExit().get();
				LOGGER.warn("Factorio was force killed.");
			}

			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new IOException("Factorio command failed with exit code: " + exitCode);
			}
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
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
