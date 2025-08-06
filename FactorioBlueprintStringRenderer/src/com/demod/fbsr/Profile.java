package com.demod.fbsr;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.TeeOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.rapidoid.commons.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.dcba.CommandReporting;
import com.demod.dcba.CommandReporting.ExceptionWithBlame;
import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModLoader;
import com.demod.factorio.ModLoader.Mod;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.BlueprintFinder.FindBlueprintResult;
import com.demod.fbsr.app.DiscordService.ImageShrinkResult;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprintString;
import com.demod.fbsr.cli.CmdBot;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.gui.layout.GUILayoutBlueprint;
import com.demod.fbsr.gui.layout.GUILayoutBook;
import com.demod.fbsr.map.MapVersion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Profile {
	private static final Logger LOGGER = LoggerFactory.getLogger(Profile.class);

    public static final String ASSETS_ZIP_VERSION_TXT = "version.txt";
    public static final String ASSETS_ZIP_PROFILE_JSON = "profile.json";
    public static final String ASSETS_ZIP_DUMP_JSON = "dump.json";
    public static final String ASSETS_ZIP_RENDERING_JSON = "rendering.json";

    public static final Set<String> BUILTIN_MODS;
	public static final Set<String> BASE_ENTITIES;
	public static final Set<String> BASE_TILES;

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
	}

    //The BUILD statuses are named after the next step in the build process
    public static enum ProfileStatus {
        INVALID,
        DISABLED,
        READY,

        BUILD_MANIFEST, // Config Updates
        BUILD_DOWNLOAD, // Mod Updates
        BUILD_DUMP, // Factorio Updates
        BUILD_ASSETS, // Rendering Updates
        
        NEED_FACTORIO_INSTALL, // Factorio is not configured
        NEED_MOD_PORTAL_CREDENTIALS, // Mod Portal API is not configured
    }

    public static enum ProfileWarning {
        VERSION_MISMATCH,
        PROFILE_MISMATCH
    }

    private final String name;
    
    private final File folderProfile;
    private final File folderBuild;
    private final File fileAssets;

    private final File fileProfile;
    
    private final File fileManifest;

    private final File folderBuildMods;
    private final File fileModList;

    private final File folderBuildData;
    private final File fileScriptOutputDumpJson;
    private final File fileScriptOutputVersion;

    private final File folderProfileTests;
    private final File folderBuildTests;
    private final File fileBuildTestsReport;

    private FactorioData factorioData;
    private RenderingRegistry renderingRegistry;
    private AtlasPackage atlasPackage;
    private ModLoader modLoader;

    private FactorioManager factorioManager;
    private GUIStyle guiStyle;
    private IconManager iconManager;

    public static Profile byName(String name) {
        JSONObject json = Config.get().getJSONObject("factorio");
        File folderProfileRoot = new File(json.optString("profiles", "profiles"));
        File folderBuildRoot = new File(json.optString("build", "build"));
        File folderAssets = new File(json.optString("assets", "assets"));

        return new Profile(
                new File(folderProfileRoot, name), 
                new File(folderBuildRoot, name), 
                new File(folderAssets, name + ".zip"));
    }

    public static Profile vanilla() {
        return Profile.byName("vanilla");
    }

    public boolean isVanilla() {
        return folderProfile.getName().equals("vanilla");
    }

    public Profile(File folderProfile, File folderBuild, File fileAssets) {
        name = folderProfile.getName();

        this.folderProfile = folderProfile;
        this.folderBuild = folderBuild;
        this.fileAssets = fileAssets;

        fileProfile = new File(folderProfile, "profile.json");

        fileManifest = new File(folderBuild, "manifest.json");

        folderBuildMods = new File(folderBuild, "mods");
        fileModList = new File(folderBuildMods, "mod-list.json");

        folderBuildData = new File(folderBuild, "data");
        File folderScriptOutput = new File(folderBuildData, "script-output");
        fileScriptOutputDumpJson = new File(folderScriptOutput, "data-raw-dump.json");
        fileScriptOutputVersion = new File(folderScriptOutput, "version.txt");

        folderProfileTests = new File(folderProfile, "tests");
        folderBuildTests = new File(folderBuild, "tests");
        fileBuildTestsReport = new File(folderBuildTests, "test-report.txt");

        resetLoadedData();
    }

    public void resetLoadedData() {
        factorioData = new FactorioData(fileAssets);
        renderingRegistry = new RenderingRegistry(this);
        atlasPackage = new AtlasPackage();

        if (FactorioManager.hasFactorioInstall()) {
            modLoader = new ModLoader(FactorioManager.getFactorioInstall(), folderBuildMods);
        } else {
            modLoader = null;
        }

        factorioManager = null;
        guiStyle = null;
        iconManager = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Profile other = (Profile) obj;
        try {
            return Files.isSameFile(folderProfile.toPath(), other.folderProfile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int hashCode() {
        return folderProfile.hashCode();
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

    public File getFileAssets() {
        return fileAssets;
    }

    public File getFileDumpDataJson() {
        return fileScriptOutputDumpJson;
    }

    public FactorioData getFactorioData() {
        return factorioData;
    }

    public AtlasPackage getAtlasPackage() {
        return atlasPackage;
    }

    public ModLoader getModLoader() {
        return modLoader;
    }

    public FactorioManager getFactorioManager() {
        return factorioManager;
    }

    public GUIStyle getGuiStyle() {
        return guiStyle;
    }

    public IconManager getIconManager() {
        return iconManager;
    }

    public RenderingRegistry getRenderingRegistry() {
        return renderingRegistry;
    }

    public void setFactorioManager(FactorioManager factorioManager) {
        this.factorioManager = factorioManager;
    }

    public void setGuiStyle(GUIStyle guiStyle) {
        this.guiStyle = guiStyle;
    }

    public void setIconManager(IconManager iconManager) {
        this.iconManager = iconManager;
    }

    public void setFactorioData(FactorioData factorioData) {
        this.factorioData = factorioData;
    }

    public static List<Profile> listProfiles() {
        JSONObject jsonFactorioManager = Config.get().getJSONObject("factorio");
        File folderProfileRoot = new File(jsonFactorioManager.optString("profiles", "profiles"));
        File folderBuildRoot = new File(jsonFactorioManager.optString("build", "build"));
        File folderAssets = new File(jsonFactorioManager.optString("assets", "assets"));

        if (!folderProfileRoot.exists()) {
            return ImmutableList.of();
        }

        List<Profile> profiles = new ArrayList<>();

        for (File folderProfile : folderProfileRoot.listFiles()) {
            String name = folderProfile.getName();
            
            if (name.equals(".git") || !folderProfile.isDirectory()) {
                continue;
            }
            
            Profile profile = new Profile(
                    folderProfile, 
                    new File(folderBuildRoot, name), 
                    new File(folderAssets, name + ".zip"));
            profiles.add(profile);
        }

        if (folderAssets.exists()) {
            for (File fileAssets : folderAssets.listFiles()) {
                if (!fileAssets.getName().endsWith(".zip")) {
                    continue;
                }
                String name = fileAssets.getName().substring(0, fileAssets.getName().length() - 4);
                
                if (profiles.stream().anyMatch(p -> p.getName().equals(name))) {
                    continue;
                }
                
                Profile profile = new Profile(
                        new File(folderProfileRoot, name),
                        new File(folderBuildRoot, name),
                        fileAssets);
                profiles.add(profile);
            }
        }

        return profiles;
    }

    public boolean isValid() {
        return fileProfile.exists();
    }

    public boolean isReady() {
        return getStatus() == ProfileStatus.READY;
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
        return fileScriptOutputDumpJson.exists() && fileScriptOutputVersion.exists();
    }

    public boolean hasAssets() {
        return fileAssets.exists();
    }

    public String getStateCode() {
        if (!isValid()) {
            return "[XXXX]";
        }

        boolean hasWarnings = !getWarnings().isEmpty();
        String prefix = hasWarnings ? "!" : "[";
        String postfix = hasWarnings ? "!" : "]";

        return prefix
            + (hasManifest() ? "M" : "-")
            + (hasDownloaded() ? "D" : "-")
            + (hasDump() ? "U" : "-")
            + (hasAssets() ? "A" : "-")
            + postfix;
    }

    public List<ProfileWarning> getWarnings() {
        if (!isValid()) {
            return ImmutableList.of();
        }

        if (!isEnabled()) {
            return ImmutableList.of();
        } 
        
        List<ProfileWarning> warnings = new ArrayList<>();

        if (hasVersionMismatch()) {
            warnings.add(ProfileWarning.VERSION_MISMATCH);
        }

        if (hasProfileMismatch()) {
            warnings.add(ProfileWarning.PROFILE_MISMATCH);
        }

        return warnings;
    }

    public ProfileStatus getStatus() {

        if (!isValid()) {
            return ProfileStatus.INVALID;

        } else if (!isEnabled()) {
            return ProfileStatus.DISABLED;

        } else if (hasAssets()) {
            return ProfileStatus.READY;

        } else if (hasDump() && hasDownloaded()) {
            if (FactorioManager.hasFactorioInstall()) {
                return ProfileStatus.BUILD_ASSETS;
            } else {
                return ProfileStatus.NEED_FACTORIO_INSTALL;
            }

        } else if ((hasManifest() && hasDownloaded())) {
            if (FactorioManager.hasFactorioInstall()) {
                return ProfileStatus.BUILD_DUMP;
            } else {
                return ProfileStatus.NEED_FACTORIO_INSTALL;
            }

        } else if (hasManifest()) {
            if (FactorioManager.hasModPortalApi()) {
                return ProfileStatus.BUILD_DOWNLOAD;
            } else {
                return ProfileStatus.NEED_MOD_PORTAL_CREDENTIALS;
            }

        } else if (isValid()) {
            return ProfileStatus.BUILD_MANIFEST;

        }
        return null;
    }

    public String getDumpFactorioVersion() {
        if (!hasDump()) {
            return null;
        }
        
        try {
            return Files.readString(fileScriptOutputVersion.toPath()).trim();
        } catch (IOException e) {
            System.out.println("Failed to read version.txt for profile: " + folderProfile.getName());
            e.printStackTrace();
            return null;
        }
    }

    public String getAssetsFactorioVersion() {
        if (!hasAssets()) {
            return null;
        }

        try (ZipFile zipFile = new ZipFile(fileAssets)) {
            
            ZipEntry entryVersion = zipFile.getEntry(ASSETS_ZIP_VERSION_TXT);
            if (entryVersion == null) {
                System.out.println(ASSETS_ZIP_VERSION_TXT + " not found in " + fileAssets.getName() + " for profile: " + folderProfile.getName());
                return null;
            }

            try (InputStream is = zipFile.getInputStream(entryVersion)) {
                return new String(is.readAllBytes()).trim();
            }

        } catch (IOException e) {
            System.out.println("Failed to read " + ASSETS_ZIP_VERSION_TXT + " from " + fileAssets.getName() + " for profile: " + folderProfile.getName());
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject getAssetsRenderingConfiguration() {
        if (!hasAssets()) {
            return null;
        }

        try (ZipFile zipFile = new ZipFile(fileAssets)) {

            ZipEntry entryVersion = zipFile.getEntry(ASSETS_ZIP_RENDERING_JSON);
            if (entryVersion == null) {
                System.out.println(ASSETS_ZIP_RENDERING_JSON + " not found in " + fileAssets.getName() + " for profile: " + folderProfile.getName());
                return null;
            }

            try (InputStream is = zipFile.getInputStream(entryVersion)) {
                try {
                    return new JSONObject(new JSONTokener(is));
                } catch (JSONException e) {
                    System.out.println("Failed to parse " + ASSETS_ZIP_RENDERING_JSON + " from " + fileAssets.getName() + " for profile: " + folderProfile.getName());
                    return null;
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to read " + ASSETS_ZIP_RENDERING_JSON + " from " + fileAssets.getName() + " for profile: " + folderProfile.getName());
            e.printStackTrace();
            return null;
        }
    }

    public boolean hasVersionMismatch() {
        List<String> versions = new ArrayList<>();
        if (hasAssets()) {
            versions.add(getAssetsFactorioVersion());
        }
        if (hasDump()) {
            versions.add(getDumpFactorioVersion());
        }
        if (FactorioManager.hasFactorioInstall()) {
            versions.add(FactorioManager.getFactorioVersion());
        }
        if (versions.isEmpty()) {
            return false;
        }
        String firstVersion = versions.get(0);
        return versions.stream().anyMatch(version -> !version.equals(firstVersion));
    }

    public boolean hasProfileMismatch() {
        if (!isValid() || !hasAssets()) {
            return false;
        }
        JSONObject jsonProfile = readJsonFile(fileProfile);
        try (ZipFile zipFile = new ZipFile(fileAssets)) {
            ZipEntry entryProfile = zipFile.getEntry(ASSETS_ZIP_PROFILE_JSON);
            if (entryProfile == null) {
                System.out.println(ASSETS_ZIP_PROFILE_JSON + " not found in assets.zip for profile: " + folderProfile.getName());
                return true;
            }
            try (InputStream is = zipFile.getInputStream(entryProfile)) {
                JSONObject jsonAssetsProfile = new JSONObject(new JSONTokener(is));
                return !jsonProfile.similar(jsonAssetsProfile);
            }
        } catch (IOException | JSONException e) {
            System.out.println("Failed to read " + ASSETS_ZIP_PROFILE_JSON + " from assets.zip for profile: " + folderProfile.getName());
            e.printStackTrace();
            return true;
        }
    }

    public boolean setEnabled(boolean enabled) {
        if (!isValid()) {
            return false;
        }

        JSONObject jsonProfile = readJsonFile(fileProfile);
        jsonProfile.put("enabled", enabled);

        if (!writeProfileSortedJsonFile(fileProfile, jsonProfile)) {
            System.out.println("Failed to write profile.json for profile: " + folderProfile.getName());
            return false;
        }

        return true;
    }

    public boolean generateProfile(String... mods) {
        if (isValid()) {
            System.out.println("Profile " + folderProfile.getName() + " already exists.");
            return false;
        }
        
        JSONObject jsonProfile = new JSONObject();
        jsonProfile.put("enabled", true);
        JSONArray jsonMods = new JSONArray();
        for (String mod : mods) {
            jsonMods.put(mod);
        }
        jsonProfile.put("mods", jsonMods);
        jsonProfile.put("mod-overrides", new JSONObject());
        jsonProfile.put("entity-overrides", new JSONObject());
        jsonProfile.put("tile-overrides", new JSONObject());

        if (!writeProfileSortedJsonFile(fileProfile, jsonProfile)) {
            System.out.println("Failed to write profile.json for profile: " + folderProfile.getName());
            return false;
        }

        System.out.println("Profile created: " + folderProfile.getAbsolutePath());
        return true;
    }

    public static boolean generateDefaultVanillaProfile(boolean force) {
        Profile editor = Profile.vanilla();
        if (editor.isValid()) {
            if (!force) {
                System.out.println("Default vanilla profile already exists.");
                return false;
            }
            if (!editor.delete()) {
                System.out.println("Failed to delete old vanilla profile: " + editor.getFolderProfile().getAbsolutePath());
                return false;
            }
        }
        
        File folderProfile = editor.getFolderProfile();
        folderProfile.mkdirs();
        try (InputStream is = Profile.class.getClassLoader().getResourceAsStream("profile-vanilla.json")) {
            Files.copy(is, new File(folderProfile, "profile.json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private Optional<JSONObject> generateRenderingConfiguration(Profile profileVanilla) {
        resetLoadedData();
        try {

            if (!isValid()) {
                System.out.println("Profile is not valid: " + folderProfile.getName());
                return Optional.empty();
            }

            if (!hasDump()) {
                System.out.println("Profile does not have a dump file: " + folderProfile.getName());
                return Optional.empty();
            }

            if (!FactorioManager.hasFactorioInstall()) {
                System.out.println("Factorio needs to be installed and configured!");
                return Optional.empty();
            }

            if (!hasDownloaded()) {
                System.out.println("Profile has not downloaded mods yet: " + folderProfile.getName());
                return Optional.empty();
            }
            
            List<Profile> profiles = new ArrayList<>();
            profiles.add(this);
            Set<String> ignoredEntities = new HashSet<>();
            Set<String> ignoredTiles = new HashSet<>();
            if (!profileVanilla.equals(this)) {
                if (!profileVanilla.hasAssets()) {
                    System.out.println("Vanilla profile does not have generated assets.");
                    return Optional.empty();
                }

                JSONObject jsonRenderingVanilla = profileVanilla.getAssetsRenderingConfiguration();
                if (jsonRenderingVanilla == null) {
                    return Optional.empty();
                }

                profiles.add(profileVanilla);
                ignoredEntities.addAll(jsonRenderingVanilla.getJSONObject("entities").keySet());
                ignoredTiles.addAll(jsonRenderingVanilla.getJSONObject("tiles").keySet());
            }

            JSONObject jsonProfile = readJsonFile(fileProfile);
            JSONObject jsonProfileModOverrides = jsonProfile.optJSONObject("mod-overrides", new JSONObject());
            JSONObject jsonProfileEntityOverrides = jsonProfile.optJSONObject("entity-overrides", new JSONObject());
            JSONObject jsonProfileTileOverrides = jsonProfile.optJSONObject("tile-overrides", new JSONObject());
            
            FactorioData factorioData = new FactorioData(fileScriptOutputDumpJson, getDumpFactorioVersion());
            if (!factorioData.initialize(false)) {
                System.out.println("Failed to initialize Factorio data for profile: " + folderProfile.getName());
                return Optional.empty();
            }
            
            ModLoader modLoader = new ModLoader(FactorioManager.getFactorioInstall(), folderBuildMods);

            for (String modName : modLoader.getMods().keySet().stream().sorted().collect(Collectors.toList())) {
                System.out.println("Mod " + modName + " (" + modLoader.getMod(modName).get().getInfo().getTitle() + ")");
            }
            
            DataTable table = factorioData.getTable();
            JSONObject jsonRendering = new JSONObject();
            Utils.terribleHackToHaveOrderedJSONObject(jsonRendering);
            AtomicBoolean failure = new AtomicBoolean(false);

            for (String name : jsonProfileEntityOverrides.keySet()) {
                if (!table.getEntity(name).isPresent()) {
                    System.out.println("Entity override for unknown entity: " + name);
                    failure.set(true);
                }
            }
            for (String name : jsonProfileTileOverrides.keySet()) {
                if (!table.getTile(name).isPresent()) {
                    System.out.println("Tile override for unknown tile: " + name);
                    failure.set(true);
                }
            }

            Function<List<String>, List<String>> convertNames = modNames -> {
                List<String> mods = new ArrayList<>();
                for (String modName : modNames) {
                    if (modName.equals("base") || modName.equals("core")) {
                        continue;
                    }
                    if (jsonProfileModOverrides.has(modName)) {
                        JSONObject jsonMod = jsonProfileModOverrides.getJSONObject(modName);
                        if (jsonMod.has("title")) {
                            String title = jsonMod.getString("title");
                            if (!mods.contains(title)) {
                                mods.add(title);
                                continue;
                            }
                        }
                        if (jsonMod.has("name")) {
                            modName = jsonMod.getString("name");
                        }
                    }
                    Optional<Mod> optMod = modLoader.getMod(modName);
                    if (!optMod.isPresent()) {
                        mods.add(modName);
                        continue;
                    }
                    String modTitle = optMod.get().getInfo().getTitle();
                    if (!mods.contains(modTitle)) {
                        mods.add(modTitle);
                    }
                }
                return mods;
            };

            JSONObject jsonRenderingEntities = new JSONObject();
            Utils.terribleHackToHaveOrderedJSONObject(jsonRenderingEntities);
            jsonRendering.put("entities", jsonRenderingEntities);
            table.getEntities().values().stream()
                    .filter(e -> {
                        if (jsonProfileEntityOverrides.has(e.getName())) {
                            if (jsonProfileEntityOverrides.isNull(e.getName())) {
                                return false;
                            }
                            return true;
                        }
                        return isBlueprintable(e);
                    })
                    .sorted(Comparator.comparing(e -> e.getName())).forEach(e -> {
                        if (ignoredEntities.contains(e.getName())) {
                            return;
                        }

                        Optional<Class<? extends EntityRendererFactory>> factoryClass = EntityRendererFactory.findFactoryClassByType(e.getType());

                        Optional<Collection<String>> overrideMods = Optional.empty();
                        if (jsonProfileEntityOverrides.has(e.getName())) {
                            if (jsonProfileEntityOverrides.isNull(e.getName())) {
                                return;
                            }

                            JSONObject jsonOverride = jsonProfileEntityOverrides.getJSONObject(e.getName());
                            if (jsonOverride.has("rendering")) {
                                String renderingClassName = jsonOverride.getString("rendering");
                                factoryClass = EntityRendererFactory.findFactoryClassByName(renderingClassName);
                                if (!factoryClass.isPresent()) {
                                    System.out.println("Entity rendering class override for " + e.getName() + " not found: " + renderingClassName);
                                    failure.set(true);
                                    return;
                                }
                            }
                            if (jsonOverride.has("mods")) {
                                JSONArray jsonMods = jsonOverride.getJSONArray("mods");
                                overrideMods = Optional.of(jsonMods.toList().stream()
                                        .map(Object::toString).collect(Collectors.toList()));
                            } 
                        }

                        if (!factoryClass.isPresent()) {
                            System.out.println("Entity rendering class for " + e.getName() + " not found: " + e.getType());
                            failure.set(true);
                            return;
                        }

                        List<String> modNames = new ArrayList<>();
                        if (overrideMods.isPresent()) {
                            for (String modName : overrideMods.get()) {
                                if (!modNames.contains(modName)) {
                                    modNames.add(modName);
                                }
                            }

                        } else {
                            try {
                                EntityRendererFactory factory = factoryClass.get().getConstructor().newInstance();
                                factory.setProfile(this);
                                factory.setPrototype(e);
                                factory.initFromPrototype();
                                factory.initAtlas(def -> {
                                    String modName = def.getModName();
                                    if (!modNames.contains(modName)) {
                                        modNames.add(modName);
                                    }
                                });

                                if (e.getName().equals("car-vehicle-machine-gun")) {
                                    System.out.println("DEBUG");
                                }

                                IconLayer.fromPrototype(factory.prototype.lua()).forEach(layer -> {
                                    String modName = layer.getModName();
                                    if (!modNames.contains(modName)) {
                                        modNames.add(modName);
                                    }
                                });

                                factory.prototype.getTable().getItem(e.getName()).ifPresent(p -> {
                                    IconLayer.fromPrototype(p.lua()).forEach(layer -> {
                                        String modName = layer.getModName();
                                        if (!modNames.contains(modName)) {
                                            modNames.add(modName);
                                        }
                                    });
                                });

                            } catch (Exception ex) {
                                System.out.println("Failed to initialize entity renderer factory for " + e.getName() + ": " + factoryClass.get().getSimpleName());
                                ex.printStackTrace();
                                failure.set(true);
                                return;
                            }
                        }

                        List<String> mods = convertNames.apply(modNames);
                        if (mods.isEmpty() && !isVanilla()) {
                            System.out.println("Entity " + e.getName() + " (" + factoryClass.get().getSimpleName() + ") has no mods associated with it in a non-vanilla profile!");
                            failure.set(true);
                            return;
                        }
                        
                        JSONObject jsonRenderingEntity = new JSONObject();
                        Utils.terribleHackToHaveOrderedJSONObject(jsonRenderingEntity);
                        jsonRenderingEntity.put("rendering", factoryClass.get().getName());
                        jsonRenderingEntity.put("mods", new JSONArray(mods));
                        jsonRenderingEntities.put(e.getName(), jsonRenderingEntity);

                        System.out.println(mods.stream().collect(Collectors.joining(", ", "[", "]")) + " Entity " + e.getName() + " (" + factoryClass.get().getSimpleName() + ")");
                    });

            JSONObject jsonRenderingTiles = new JSONObject();
            Utils.terribleHackToHaveOrderedJSONObject(jsonRenderingTiles);
            jsonRendering.put("tiles", jsonRenderingTiles);
            table.getTiles().values().stream()
                    .filter(t -> {
                        if (jsonProfileTileOverrides.has(t.getName())) {
                            if (jsonProfileTileOverrides.isNull(t.getName())) {
                                return false;
                            }
                            return true;
                        }
                        return isBlueprintable(t);
                    })
                    .sorted(Comparator.comparing(t -> t.getName())).forEach(t -> {
                        if (ignoredTiles.contains(t.getName())) {
                            return;
                        }

                        Optional<Collection<String>> overrideMods;
                        if (jsonProfileTileOverrides.has(t.getName())) {
                            if (jsonProfileTileOverrides.isNull(t.getName())) {
                                return;
                            }

                            JSONObject jsonOverride = jsonProfileTileOverrides.getJSONObject(t.getName());
                            if (jsonOverride.has("mods")) {
                                JSONArray jsonMods = jsonOverride.getJSONArray("mods");
                                overrideMods = Optional.of(jsonMods.toList().stream()
                                        .map(Object::toString).collect(Collectors.toList()));
                            } else {
                                overrideMods = Optional.empty();
                            }
                            
                        } else {
                            overrideMods = Optional.empty();
                        }

                        List<String> modNames = new ArrayList<>();
                        if (overrideMods.isPresent()) {
                            for (String modName : overrideMods.get()) {
                                if (!modNames.contains(modName)) {
                                    modNames.add(modName);
                                }
                            }

                        } else {
                            try {
                                TileRendererFactory factory = new TileRendererFactory();
                                factory.setProfile(this);
                                factory.setPrototype(t);
                                factory.initFromPrototype(table);
                                factory.initAtlas(def -> {
                                    String modName = def.getModName();
                                    if (!modNames.contains(modName)) {
                                        modNames.add(modName);
                                    }
                                });

                                IconLayer.fromPrototype(factory.prototype.lua()).forEach(layer -> {
                                    String modName = layer.getModName();
                                    if (!modNames.contains(modName)) {
                                        modNames.add(modName);
                                    }
                                });

                                factory.prototype.getTable().getItem(t.getName()).ifPresent(p -> {
                                    IconLayer.fromPrototype(p.lua()).forEach(layer -> {
                                        String modName = layer.getModName();
                                        if (!modNames.contains(modName)) {
                                            modNames.add(modName);
                                        }
                                    });
                                });

                            } catch (Exception ex) {
                                System.out.println("Failed to initialize tile renderer factory for " + t.getName() + ": " + t.getType());
                                ex.printStackTrace();
                                failure.set(true);
                                return;
                            }
                        }

                        List<String> mods = convertNames.apply(modNames);
                        if (mods.isEmpty() && !isVanilla()) {
                            System.out.println("Tile " + t.getName() + " has no mods associated with it in a non-vanilla profile!");
                            failure.set(true);
                            return;
                        }

                        JSONObject jsonRenderingTile = new JSONObject();
                        Utils.terribleHackToHaveOrderedJSONObject(jsonRenderingTile);
                        jsonRenderingTile.put("mods", new JSONArray(mods));
                        jsonRenderingTiles.put(t.getName(), jsonRenderingTile);

                        System.out.println(mods.stream().collect(Collectors.joining(", ", "[", "]")) + " Tile " + t.getName());
                    });

            if (failure.get()) {
                return Optional.empty();
            }
            return Optional.of(jsonRendering);
        } finally {
            resetLoadedData();
        }
    }

    private static boolean isBlueprintable(EntityPrototype e) {
        // based on cannot_ghost() by _codegreen
        if (e.getFlags().contains("not-blueprintable")) {
            return false;
        }
        if (!e.getFlags().contains("player-creation")) {
            return false;
        }
        if (e.getPlacedBy().isEmpty()) {
            return false;
        }
        return true;
    }

    private static boolean isBlueprintable(TilePrototype t) {
        if (!t.lua().get("can_be_part_of_blueprint").optboolean(true)) {
            return false;
        }
        if (t.getPlacedBy().isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean cleanManifest() {
        return !fileManifest.exists() || fileManifest.delete();
    }

    public boolean cleanAllDownloads() {
        if (!folderBuildMods.exists()) {
            return false;
        }
         
        for (File file : folderBuildMods.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                if (!file.delete()) {
                    System.out.println("Failed to delete file: " + file.getAbsolutePath());
                    return false;
                }   
            }
        }

        if (fileModList.exists() && !fileModList.delete()) {
            System.out.println("Failed to delete mod list file: " + fileModList.getAbsolutePath());
            return false;
        }

        if (modLoader != null) {
            modLoader.reload();
        }

        return true;
    }

    public boolean cleanInvalidDownloads() {
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

        boolean changed = false;
        Set<String> validZips = jsonZips.keySet();
        if (folderBuildMods.exists()) {
            for (File file : folderBuildMods.listFiles()) {
                if (!validZips.contains(file.getName())) {
                    System.out.println("Deleting invalid download: " + file.getName());
                    if (!file.delete()) {
                        System.out.println("Failed to delete file: " + file.getAbsolutePath());
                        return false;
                    }
                    changed = true;
                }
            }
        }

        if (changed && modLoader != null) {
            modLoader.reload();
        }

        return true;
    }

    public boolean cleanDump() {
        if (fileScriptOutputDumpJson.exists() && !fileScriptOutputDumpJson.delete()) {
            System.out.println("Failed to delete dump file: " + fileScriptOutputDumpJson.getAbsolutePath());
            return false;
        }
        if (fileScriptOutputVersion.exists() && !fileScriptOutputVersion.delete()) {
            System.out.println("Failed to delete version file: " + fileScriptOutputVersion.getAbsolutePath());
            return false;
        }
        return true;
    }

    public boolean deleteAssets() {
        if (fileAssets.exists() && !fileAssets.delete()) {
            System.out.println("Failed to delete assets file: " + fileAssets.getAbsolutePath());
            return false;
        }
        return true;
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
                            System.out.println("Failed to delete: " + file.getAbsolutePath());
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
                success.set(false);
                System.out.println("Failed to delete: " + e.getMessage());
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
                            System.out.println("Failed to delete: " + file.getAbsolutePath());
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
                success.set(false);
                System.out.println("Failed to delete: " + e.getMessage());
            }
        }

        return success.get();
    }

    public boolean deleteBuild() {
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
                            System.out.println("Failed to delete: " + file.getAbsolutePath());
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
                success.set(false);
                System.out.println("Failed to delete: " + e.getMessage());
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
        JSONArray jsonProfileMods = jsonProfile.optJSONArray("mods");

        List<String> mods = new ArrayList<>();
        Map<String, String> modVersions = new HashMap<>();
        mods.add("base"); // Always include base mod

        for (int i = 0; i < jsonProfileMods.length(); i++) {
            String dependency = jsonProfileMods.getString(i).trim();

            String modName;
            Optional<String> modVersion;
            if (dependency.contains("=")) {
                String[] parts = dependency.split("=", 2);
                modName = parts[0].trim();
                modVersion = Optional.of(parts[1].trim());
            } else {
                modName = dependency.trim();
                modVersion = Optional.empty();
            }

            if (modVersion.isPresent()) {
                modVersions.put(modName, modVersion.get());
            }

            if (!mods.contains(modName)) {
                mods.add(modName);
            }
        }

        Map<String, JSONObject> modInfo = new LinkedHashMap<>();
        for (String modName : mods) {
            if (BUILTIN_MODS.contains(modName)) {
                modInfo.put(modName, new JSONObject());
                continue;
            }
            try {
                walkDependencies(modInfo, modName, modVersions);
            } catch (IOException e) {
                System.out.println("Failed to walk dependencies for mod: " + modName);
                e.printStackTrace();
                return false;
            }
        }

        JSONObject jsonManifest = new JSONObject();
        JSONObject jsonZips = new JSONObject();
        jsonManifest.put("zips", jsonZips);
        for (Entry<String, JSONObject> entry : modInfo.entrySet()) {
            String modName = entry.getKey();

            if (BUILTIN_MODS.contains(modName)) {
                continue;
            }

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
                System.out.println("Invalid mod file name: " + filename + ". Must end with .zip");
                return false;
            }
        }

        JSONArray jsonModList = new JSONArray();
        jsonManifest.put("mod_list", jsonModList);
        for (String modName : modInfo.keySet()) {
            jsonModList.put(modName);
        }


        if (!folderBuild.exists()) {
            folderBuild.mkdirs();
        }

        if (!writeJsonFile(fileManifest, jsonManifest)) {
            System.out.println("Failed to write manifest.json for profile: " + folderProfile.getName());
            return false;
        }

        cleanInvalidDownloads();
        cleanDump();
        deleteAssets();

        return true;
    }

    private static void walkDependencies(Map<String, JSONObject> results, String mod, Map<String, String> modVersions) throws IOException {
		JSONObject jsonRelease;
        if (modVersions.containsKey(mod)) {
            jsonRelease = FactorioModPortal.findModReleaseInfoFull(mod, modVersions.get(mod));
        } else {
            jsonRelease = FactorioModPortal.findLatestModReleaseInfoFull(mod);
        }

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
                //TODO we need to solve specific version dependencies
				walkDependencies(results, depMod, modVersions);
			}
		}
	}

    public boolean buildDownload() {

        if (!hasManifest()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a manifest.");
            return false;
        }

        if (!FactorioManager.hasModPortalApi()) {
            System.out.println("Mod Portal API is not configured. Cannot download mods.");
            return false;
        }

        folderBuildMods.mkdirs();

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
                if (!target.exists()) {
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

        cleanInvalidDownloads();
        cleanDump();
        deleteAssets();

        if (modLoader != null) {
            modLoader.reload();
        }

        return true;
    }

    public boolean buildDump(boolean force) {

        if (!hasManifest()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a manifest.");
            return false;
        }

        if (!hasDownloaded()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have the correct mods downloaded.");
            return false;
        }

        if (!FactorioManager.hasFactorioInstall()) {
            System.out.println("Factorio is not configured. Cannot generate dump file.");
            return false;
        }

        if (!updateModList()) {
            System.out.println("Failed to update mod-list.json for profile: " + folderProfile.getName());
            return false;
        }

        folderBuildData.mkdirs();

        if (force && fileScriptOutputDumpJson.exists()) {
            if (!fileScriptOutputDumpJson.delete()) {
                System.out.println("Failed to delete old dump file: " + fileScriptOutputDumpJson.getAbsolutePath());
                return false;
            }
        }

        File factorioInstall = FactorioManager.getFactorioInstall();
        Optional<File> factorioExecutableOverride = FactorioManager.getFactorioExecutableOverride();

        if (!FactorioData.generateDumpAndVersion(folderBuildData, folderBuildMods, factorioInstall, factorioExecutableOverride)) {
            System.out.println("Failed to build dump file for profile: " + folderProfile.getName());
            return false;
        }

        if (!hasDump()) {
            System.out.println("Profile " + folderProfile.getName() + " was unable to generate a dump file!");
            return false;
        }

        deleteAssets();

        return true;
    }

    private boolean updateModList() {
        
        JSONObject jsonManifest = readJsonFile(fileManifest);
        JSONArray jsonManifestModList = jsonManifest.optJSONArray("mod_list");
        
        if (jsonManifestModList == null) {
            System.out.println("Manifest does not contain mod list for profile: " + folderProfile.getName());
            return false;
        }

        if (!folderBuildMods.exists()) {
            folderBuildMods.mkdirs();
        }

        JSONObject jsonModList = new JSONObject();
        JSONArray jsonModListMods = new JSONArray();
        jsonModList.put("mods", jsonModListMods);
        Set<String> modCheck = new HashSet<>();
        for (int i = 0; i < jsonManifestModList.length(); i++) {
            String modName = jsonManifestModList.getString(i).trim();
            JSONObject jsonMod = new JSONObject();
            jsonMod.put("name", modName);
            jsonMod.put("enabled", true);
            jsonModListMods.put(jsonMod);
            modCheck.add(modName);
        }
        for (String mod : BUILTIN_MODS) {
            if (!modCheck.contains(mod)) {
                JSONObject jsonMod = new JSONObject();
                jsonMod.put("name", mod);
                jsonMod.put("enabled", false);
                jsonModListMods.put(jsonMod);
            }
        }

        if (fileModList.exists()) {
            fileModList.setWritable(true);
        }

        if (!writeJsonFile(fileModList, jsonModList)) {
            System.out.println("Failed to write mod-list.json for profile: " + folderProfile.getName());
            return false;
        }

        fileModList.setWritable(false);

        return true;
    }

    public boolean buildAssets(boolean force) {

        if (!hasDump()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a dump file.");
            return false;
        }

        if (!hasDownloaded()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have the correct mods downloaded.");
            return false;
        }

        if (!FactorioManager.hasFactorioInstall()) {
            System.out.println("Factorio is not configured. Cannot build asset files.");
            return false;
        }

        if (!force && hasAssets()) {
            System.out.println("Profile " + folderProfile.getName() + " already has asset files.");
            return false;
        }

        fileAssets.getParentFile().mkdirs();

        File fileAssetsTemp = new File(fileAssets.getAbsolutePath() + ".tmp");
        if (fileAssetsTemp.exists()) {
            if (!fileAssetsTemp.delete()) {
                System.out.println("Failed to delete old assets temp file: " + fileAssetsTemp.getAbsolutePath());
                return false;
            }
        }

        if (fileAssets.exists()) {
            if (!fileAssets.delete()) {
                System.out.println("Failed to delete old assets file: " + fileAssets.getAbsolutePath());
                return false;
            }
        }

        boolean success = false;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileAssetsTemp))) {
            zos.setLevel(Deflater.BEST_COMPRESSION);
            
            if (!copyIntoZip(zos, fileProfile, ASSETS_ZIP_PROFILE_JSON)) {
                return false;
            }
            if (!copyIntoZip(zos, fileScriptOutputDumpJson, ASSETS_ZIP_DUMP_JSON)) {
                return false;
            }
            if (!copyIntoZip(zos, fileScriptOutputVersion, ASSETS_ZIP_VERSION_TXT)) {
                return false;
            }
            
            JSONObject jsonRendering;
            {
                Profile profileVanilla = Profile.vanilla();
                Optional<JSONObject> optJsonRendering = generateRenderingConfiguration(profileVanilla);
                if (!optJsonRendering.isPresent()) {
                    System.out.println("Failed to generate rendering configuration for profile: " + folderProfile.getName());
                    return false;
                }
                jsonRendering = optJsonRendering.get();
                ZipEntry entryRendering = new ZipEntry(ASSETS_ZIP_RENDERING_JSON);
                zos.putNextEntry(entryRendering);
                zos.write(jsonRendering.toString(2).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            if (!FBSR.populateAssets(this, jsonRendering, zos)) {
                System.out.println("Failed to populate assets for profile: " + folderProfile.getName());
                return false;
            }

            zos.close();

            if (!fileAssetsTemp.renameTo(fileAssets)) {
                System.out.println("Failed to rename temp assets file to final assets file: " + fileAssetsTemp.getAbsolutePath() + " ==> " + fileAssets.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            System.out.println("Failed to create assets zip for profile: " + folderProfile.getName());
            e.printStackTrace();
            return false;
        } finally {
            if (fileAssetsTemp.exists() && !fileAssetsTemp.delete()) {
                System.out.println("Failed to delete temp assets file: " + fileAssetsTemp.getAbsolutePath());
            }
        }

        return true;
    }

    private static boolean copyIntoZip(ZipOutputStream zos, File file, String zipEntryName) {
        if (!file.exists()) {
            System.out.println("File " + file.getAbsolutePath() + " does not exist. Cannot copy into zip.");
            return false;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(zipEntryName);
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            zos.closeEntry();
        } catch (IOException e) {
            System.out.println("Failed to copy " + file.getAbsolutePath() + " into zip as " + zipEntryName);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean runFactorio() {

        if (!hasManifest()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have a manifest.");
            return false;
        }

        if (!hasDownloaded()) {
            System.out.println("Profile " + folderProfile.getName() + " does not have the correct mods downloaded.");
            return false;
        }

        if (!FactorioManager.hasFactorioInstall()) {
            System.out.println("Factorio is not configured. Cannot run factorio.");
            return false;
        }

        if (!updateModList()) {
            System.out.println("Failed to update mod-list.json for profile: " + folderProfile.getName());
            return false;
        }
        
        try {
            JSONObject fdConfig = new JSONObject();
            folderBuildData.mkdirs();

            File factorioInstall = FactorioManager.getFactorioInstall();
		    File factorioExecutable = FactorioManager.getFactorioExecutable();

            File fileConfig = new File(folderBuildData, "config.ini");
            try (PrintWriter pw = new PrintWriter(fileConfig)) {
                pw.println("[path]");
                pw.println("read-data=" + new File(factorioInstall, "data").getAbsolutePath());
                pw.println("write-data=" + folderBuildData.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(factorioExecutable.getAbsolutePath(), "--config",
					fileConfig.getAbsolutePath(), "--mod-directory", folderBuildMods.getAbsolutePath());
			pb.directory(factorioInstall);

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

        if (!isValid()) {
            System.out.println("Profile " + folderProfile.getName() + " is not valid.");
            return ImmutableList.of();
        }

        List<String> mods = new ArrayList<>();

        if (hasManifest()) {
            JSONObject jsonManifest = readJsonFile(fileManifest);
            JSONArray jsonModList = jsonManifest.getJSONArray("mod_list");
            for (int i = 0; i < jsonModList.length(); i++) {
                String mod = jsonModList.getString(i);
                mods.add(mod);
            }

        } else {
            JSONObject jsonProfile = readJsonFile(fileProfile);
            JSONArray jsonProfileMods = jsonProfile.optJSONArray("mods");
            if (jsonProfileMods != null) {
                for (int i = 0; i < jsonProfileMods.length(); i++) {
                    String modName = jsonProfileMods.getString(i).trim();
                    mods.add(modName);
                }
            } else {
                System.out.println("No mods found in profile: " + folderProfile.getName());
            }
        }
        
        Collections.sort(mods);
        return mods;
    }

    private static JSONObject readJsonFile(File file) {
        try {
            return new JSONObject(Files.readString(file.toPath()));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean writeJsonFile(File file, JSONObject json) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(json.toString(2));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean writeProfileSortedJsonFile(File file, JSONObject json) {
        try (FileWriter fw = new FileWriter(file)) {
            List<String> preferredOrder = Arrays.asList("enabled", "mods", "mod-overrides", "entity-overrides", "tile-overrides");

            JSONObject sorted = new JSONObject();
            Utils.terribleHackToHaveOrderedJSONObject(sorted);

            for (String key : preferredOrder) {
                if (json.has(key)) {
                    Object value = json.get(key);
                    sorted.put(key, sortJsonRecursively(value));
                }
            }

            List<String> remainingKeys = new ArrayList<>();
            for (String key : json.keySet()) {
                if (!preferredOrder.contains(key)) {
                    remainingKeys.add(key);
                }
            }
            Collections.sort(remainingKeys);
            for (String key : remainingKeys) {
                Object value = json.get(key);
                sorted.put(key, sortJsonRecursively(value));
            }

            fw.write(sorted.toString(2));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Object sortJsonRecursively(Object value) {
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            List<String> keys = new ArrayList<>(obj.keySet());
            Collections.sort(keys);
            JSONObject sorted = new JSONObject();
            Utils.terribleHackToHaveOrderedJSONObject(sorted);
            for (String key : keys) {
                sorted.put(key, sortJsonRecursively(obj.get(key)));
            }
            return sorted;
        } else if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            JSONArray sortedArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                sortedArr.put(sortJsonRecursively(arr.get(i)));
            }
            return sortedArr;
        } else {
            return value;
        }
    }

    public boolean deleteTests() {
        AtomicBoolean success = new AtomicBoolean(true);

        // Delete all files and subdirectories in the tests folder
        if (folderBuildTests.exists()) {
            try {
                Files.walk(folderBuildTests.toPath())
                    .map(Path::toFile)
                    .sorted((f1, f2) -> f2.getAbsolutePath().length() - f1.getAbsolutePath().length()) // Delete children first
                    .forEach(file -> {
                        if (!file.delete()) {
                            success.set(false);
                            System.out.println("Failed to delete: " + file.getAbsolutePath());
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
                success.set(false);
                System.out.println("Failed to delete: " + e.getMessage());
            }
        }

        return success.get();
    }

    public boolean renderTests(boolean openFolder) {

        if (!hasAssets()) {
            System.out.println("Profile " + getName() + " does not have asset files.");
            return false;
        }

        if (!folderProfileTests.exists()) {
            System.out.println("Profile " + getName() + " does not have a tests folder.");
            return true;
        }

        List<File> testFiles = Arrays.asList(folderProfileTests.listFiles());
        if (testFiles.isEmpty()) {
            System.out.println("No test files found in profile: " + getName());
            return true;
        }

        List<Profile> profiles;
        if (isVanilla()) {
            profiles = ImmutableList.of(this);
        } else {
            profiles = ImmutableList.of(this, vanilla());
        }

        folderBuildTests.mkdirs();

        try (PrintWriter pw = new PrintWriter(fileBuildTestsReport)) {
            Consumer<String> report = message -> {
                pw.println(message);
                System.out.println(message);
            };

            report.accept("Rendering tests for profile: " + getName());
            report.accept("Test time: " + Instant.now());
            report.accept("Test files:");
            for (File testFile : testFiles) {
                report.accept(" - " + testFile.getName());
            }
            report.accept("");
            report.accept("/////////////////////////////////////////////");
            report.accept("");

            if (!FBSR.load(profiles)) {
                report.accept("Failed to load FBSR for profile: " + getName());
                return false;
            }

            Map<String, EntityRendererFactory> entitiesRemaining = renderingRegistry.getEntityFactories().stream().collect(Collectors.toMap(EntityRendererFactory::getName, Function.identity()));
            Map<String, TileRendererFactory> tilesRemaining = renderingRegistry.getTileFactories().stream().collect(Collectors.toMap(TileRendererFactory::getName, Function.identity()));
            Set<String> unknownEntities = new HashSet<>();
            Set<String> unknownTiles = new HashSet<>();
            List<ExceptionWithBlame> exceptions = new ArrayList<>();

            boolean failed = false;
            for (File testFile : testFiles) {
                report.accept("");

                if (!testFile.getName().endsWith(".txt")) {
                    report.accept("Skipping non-.txt test file: " + testFile.getName());
                    continue;
                }

                report.accept("Rendering test file: " + testFile.getName());
                String blueprintStringRaw;
                try {
                    blueprintStringRaw = Files.readString(testFile.toPath());
                } catch (IOException e) {
                    report.accept("Failed to read test file: " + testFile.getName() + " (" + e.getMessage() + ")");
                    failed = true;
                    continue;
                }

                List<FindBlueprintResult> searchResults = BlueprintFinder.search(blueprintStringRaw);

		        List<BSBlueprintString> blueprintStrings = searchResults.stream().flatMap(f -> f.blueprintString.stream())
				        .collect(Collectors.toList());

                if (blueprintStrings.isEmpty()) {
                    report.accept("No valid blueprint found in test file: " + testFile.getName());
                    failed = true;
                    continue;
                }

                if (blueprintStrings.size() > 1) {
                    report.accept("Can only have one blueprint string per test file: " + testFile.getName());
                    failed = true;
                    continue;
                }

                BSBlueprintString blueprintString = blueprintStrings.get(0);

                CommandReporting reporting = new CommandReporting(null, null, null);
                
                BufferedImage image = null;

                if (blueprintString.blueprintBook.isPresent()) {
                    GUILayoutBook layout = new GUILayoutBook();
                    layout.setBook(blueprintString.blueprintBook.get());
                    layout.setReporting(reporting);
                    image = layout.generateDiscordImage();

                    for (RenderResult result : layout.getResults()) {
                        result.request.getBlueprint().entities.forEach(e -> {
                            report.accept(" - " + e.name);
                            entitiesRemaining.remove(e.name);
                        });
                        result.request.getBlueprint().tiles.forEach(t -> {
                            report.accept(" - " + t.name);
                            tilesRemaining.remove(t.name);
                        });
                        result.unknownEntities.forEach(e -> {
                            report.accept("(UNKNOWN) - " + e);
                            unknownEntities.add(e);
                        });
                        result.unknownTiles.forEach(t -> {
                            report.accept("(UNKNOWN) - " + t);
                            unknownTiles.add(t);
                        });
                    }

                } else if (blueprintString.blueprint.isPresent()) {
                    GUILayoutBlueprint layout = new GUILayoutBlueprint();
                    layout.setBlueprint(blueprintString.blueprint.get());
                    layout.setReporting(reporting);
                    image = layout.generateDiscordImage();

                    layout.getResult().request.getBlueprint().entities.forEach(e -> {
                        report.accept(" - " + e.name);
                        entitiesRemaining.remove(e.name);
                    });
                    layout.getResult().request.getBlueprint().tiles.forEach(t -> {
                        report.accept(" - " + t.name);
                        tilesRemaining.remove(t.name);
                    });
                    layout.getResult().unknownEntities.forEach(e -> {
                        report.accept("(UNKNOWN) - " + e);
                        unknownEntities.add(e);
                    });
                    layout.getResult().unknownTiles.forEach(t -> {
                        report.accept("(UNKNOWN) - " + t);
                        unknownTiles.add(t);
                    });
                }

                exceptions.addAll(reporting.getExceptionsWithBlame());

                if (image == null) {
                    report.accept("Failed to generate image for test file: " + testFile.getName());
                    failed = true;
                    continue;
                }

                File fileTestOutput = new File(folderBuildTests, testFile.getName().replace(".txt", ".png"));
                try {
                    ImageIO.write(image, "png", fileTestOutput);
                    report.accept("Test output written to: " + fileTestOutput.getName());
                } catch (IOException e) {
                    report.accept("Failed to write test output: " + e.getMessage());
                    failed = true;
                    continue;
                }
            }

            report.accept("");
            report.accept("/////////////////////////////////////////////");
            report.accept("");

            if (failed) {
                report.accept("Some tests failed to render. Check the output above for details.");
                return false;
            }

            if (openFolder) {
                try {
                    Desktop.getDesktop().open(folderBuildTests);
                } catch (IOException e) {
                    report.accept("Failed to open test output folder: " + e.getMessage());
                    return false;
                }
            }
            
            boolean renderProblem = false;
                
            for (Entry<String, EntityRendererFactory> entry : entitiesRemaining.entrySet()) {
                String entityName = entry.getKey();
                EntityRendererFactory factory = entry.getValue();
                report.accept("Entity not rendered: " + factory.profile.name + " / " + factory.mods.stream().collect(Collectors.joining(",", "[","]")) + " / " + entityName);
                renderProblem = true;
            }
            for (Entry<String, TileRendererFactory> entry : tilesRemaining.entrySet()) {
                String tileName = entry.getKey();
                TileRendererFactory factory = entry.getValue();
                report.accept("Tile not rendered: " + factory.profile.name + " / " + factory.mods.stream().collect(Collectors.joining(",", "[","]")) + " / " + tileName);
                renderProblem = true;
            }

            for (String entityName : unknownEntities) {
                report.accept("Unknown entity: " + entityName);
                renderProblem = true;
            }
            for (String tileName : unknownTiles) {
                report.accept("Unknown tile: " + tileName);
                renderProblem = true;
            }

            if (!exceptions.isEmpty()) {
                report.accept("Exceptions occurred during rendering:");
                int count = 0;
                for (ExceptionWithBlame exception : exceptions) {

                    report.accept("");
                    if (exception.getBlame().isPresent()) {
                        report.accept("Blame: " + exception.getBlame().get());
                    }
                    exception.getException().printStackTrace();

                    if (++count > 5) {
                        report.accept("");
                        report.accept("... and " + (exceptions.size() - count) + " more exception(s).");
                        break;
                    }
                }
                renderProblem = true;
            }
            
            if (renderProblem) {
                report.accept("Some entities or tiles were not rendered.");
                return false;
                
            } else {
                report.accept("All entities and tiles rendered successfully for profile " + getName() + ".");
                return true;
            }

        } catch (Exception e) {
            System.out.println("Failed to render tests for profile: " + getName() + " - " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (!FBSR.unload()) {
                System.out.println("Failed to unload FBSR");
            }
        }
    }

    public boolean updateMods() {
        if (!isValid()) {
            System.out.println("Profile " + folderProfile.getName() + " is not valid.");
            return false;
        }

        JSONObject jsonProfile = readJsonFile(fileProfile);
        JSONArray jsonMods = jsonProfile.getJSONArray("mods");
        boolean changed = false;
        for (int i = 0; i < jsonMods.length(); i++) {
            String dependency = jsonMods.getString(i);
            
            String modName;
            Optional<String> modVersion;
            if (dependency.contains("=")) {
                String[] parts = dependency.split("=", 2);
                modName = parts[0].trim();
                modVersion = Optional.of(parts[1].trim());
            } else {
                modName = dependency.trim();
                modVersion = Optional.empty();
            }

            if (BUILTIN_MODS.contains(modName)) {
                continue; // Skip built-in mods
            }

            try {
                JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfo(modName);
                String latestVersion = jsonRelease.getString("version");

                if (!modVersion.equals(Optional.of(latestVersion))) {
                    jsonMods.put(i, modName + " = " + latestVersion);
                    changed = true;
                    System.out.println("Updating mod: " + modName + " from " + modVersion.orElse("<empty>") + " to " + latestVersion);
                }

            } catch (Exception e) {
                System.out.println("Failed to update mod: " + modName + " - " + e.getMessage());
                return false;
            }
        }

        if (changed) {
            writeProfileSortedJsonFile(fileProfile, jsonProfile);

            cleanManifest();
        }

        return true;
    }

    public boolean renderTestEntity(String entity, Optional<Dir16> direction, OptionalDouble orientation, Optional<String> custom) {
        
        if (!hasAssets()) {
            System.out.println("Profile " + getName() + " does not have asset files.");
            return false;
        }

        JSONObject jsonBlueprintString = new JSONObject();
        JSONObject jsonBlueprint = new JSONObject();
        jsonBlueprint.put("version", 562949955649542L);
        jsonBlueprintString.put("blueprint", jsonBlueprint);
        JSONArray jsonEntities = new JSONArray();
        jsonBlueprint.put("entities", jsonEntities);
        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("entity_number", 1);
        jsonEntity.put("name", entity);
        JSONObject jsonPosition = new JSONObject();
        jsonPosition.put("x", 0);
        jsonPosition.put("y", 0);
        jsonEntity.put("position", jsonPosition);
        jsonEntities.put(jsonEntity);

        if (direction.isPresent()) {
            jsonEntity.put("direction", direction.get().ordinal());
        }

        if (orientation.isPresent()) {
            jsonEntity.put("orientation", orientation.getAsDouble());
        }

        if (custom.isPresent()) {
            try {
                JSONObject jsonCustom = new JSONObject(custom.get());
                Utils.forEach(jsonCustom, (k, v) -> jsonEntity.put(k, v));
            } catch (JSONException e) {
                System.out.println("Failed to parse custom JSON for entity: " + e.getMessage());
                return false;
            }
        }

        String blueprintStringRaw = jsonBlueprintString.toString();
        System.out.println();
        System.out.println(blueprintStringRaw);
        System.out.println();

        BSBlueprint blueprint;
        try {
            List<FindBlueprintResult> searchResults = BlueprintFinder.search(blueprintStringRaw);
            if (searchResults.stream().anyMatch(r -> r.failureCause.isPresent())) {
                System.out.println("Failed to parse blueprint string: " + searchResults.stream()
                        .filter(r -> r.failureCause.isPresent())
                        .map(r -> r.failureCause.get().getMessage())
                        .collect(Collectors.joining(", ")));
                return false;
            }
            blueprint = searchResults.get(0).blueprintString.get().findAllBlueprints().get(0);
        } catch (Exception e) {
            System.out.println("Failed to parse blueprint string: " + e.getMessage());
            return false;
        }

        List<Profile> profiles;
        if (isVanilla()) {
            profiles = ImmutableList.of(this);
        } else {
            profiles = ImmutableList.of(this, vanilla());
        }

        folderBuildTests.mkdirs();

        if (!FBSR.load(profiles)) {
            System.out.println("Failed to load FBSR for profile: " + getName());
            return false;
        }

        try {
            FactorioManager factorioManager = FBSR.getFactorioManager();
            ModdingResolver resolver = ModdingResolver.byProfileOrder(factorioManager, profiles, false);
            
            EntityRendererFactory factory = resolver.resolveFactoryEntityName(entity);
            if (factory.isUnknown()) {
                System.out.println("Unknown entity: " + entity);
                return false;
            }

            CommandReporting reporting = new CommandReporting(null, null, null);
            RenderRequest request = new RenderRequest(blueprint, reporting);
            request.setBackground(Optional.empty());
            request.setGridLines(Optional.empty());
            request.setDontClipSprites(true);

            RenderResult result = FBSR.renderBlueprint(request);

            File fileImage = new File(folderBuildTests, name+"-" + entity + ".png");
            ImageIO.write(result.image, "PNG", fileImage);

            try {
                Desktop.getDesktop().open(folderBuildTests);
            } catch (IOException e) {
                System.out.println("Failed to open image: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            System.out.println("Failed to render " + entity + " for profile: " + getName() + " - " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally{
            if (!FBSR.unload()) {
                System.out.println("Failed to unload FBSR");
            }
        }

        return true;
    }
}
