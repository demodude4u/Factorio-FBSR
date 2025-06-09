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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModLoader;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.cli.CmdBot;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ProfileEditor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEditor.class);

    public static final Set<String> BUILTIN_MODS = ImmutableSet.of("base", "space-age", "quality", "elevated-rails");

	public static final Set<String> BASE_ENTITIES = ImmutableSet.of("accumulator", "active-provider-chest",
			"agricultural-tower", "arithmetic-combinator", "artillery-turret", "artillery-wagon",
			"assembling-machine-1", "assembling-machine-2", "assembling-machine-3", "asteroid-collector", "beacon",
			"big-electric-pole", "big-mining-drill", "biochamber", "biolab", "blue-chest", "boiler", "buffer-chest",
			"bulk-inserter", "burner-generator", "burner-inserter", "burner-mining-drill", "captive-biter-spawner",
			"capture-robot", "car", "cargo-bay", "cargo-landing-pad", "cargo-pod-container", "cargo-wagon",
			"centrifuge", "chemical-plant", "constant-combinator", "construction-robot", "crash-site-chest-1",
			"crash-site-chest-2", "crusher", "cryogenic-plant", "curved-rail-a", "curved-rail-b", "decider-combinator",
			"defender", "destroyer", "display-panel", "distractor", "dummy-rail-support", "electric-energy-interface",
			"electric-furnace", "electric-mining-drill", "electromagnetic-plant", "elevated-curved-rail-a",
			"elevated-curved-rail-b", "elevated-half-diagonal-rail", "elevated-straight-rail", "express-loader",
			"express-splitter", "express-transport-belt", "express-underground-belt", "factorio-logo-11tiles",
			"factorio-logo-16tiles", "factorio-logo-22tiles", "fast-inserter", "fast-loader", "fast-splitter",
			"fast-transport-belt", "fast-underground-belt", "flamethrower-turret", "fluid-wagon", "foundry",
			"fulgoran-ruin-attractor", "fusion-generator", "fusion-reactor", "gate", "gun-turret", "half-diagonal-rail",
			"heat-exchanger", "heat-interface", "heat-pipe", "heating-tower", "infinity-cargo-wagon", "infinity-chest",
			"infinity-pipe", "inserter", "iron-chest", "lab", "land-mine", "lane-splitter", "laser-turret",
			"legacy-curved-rail", "legacy-straight-rail", "lightning-collector", "lightning-rod", "linked-belt",
			"linked-chest", "loader", "locomotive", "logistic-robot", "long-handed-inserter", "market",
			"medium-electric-pole", "nuclear-reactor", "offshore-pump", "oil-refinery", "passive-provider-chest",
			"pipe", "pipe-to-ground", "power-switch", "programmable-speaker", "proxy-container", "pump", "pumpjack",
			"radar", "rail-chain-signal", "rail-ramp", "rail-signal", "rail-support", "railgun-turret", "recycler",
			"red-chest", "requester-chest", "roboport", "rocket-silo", "rocket-turret", "selector-combinator",
			"simple-entity-with-force", "simple-entity-with-owner", "small-electric-pole", "small-lamp", "solar-panel",
			"space-platform-hub", "spidertron", "splitter", "stack-inserter", "steam-engine", "steam-turbine",
			"steel-chest", "steel-furnace", "stone-furnace", "stone-wall", "storage-chest", "storage-tank",
			"straight-rail", "substation", "tank", "tesla-turret", "thruster", "train-stop", "transport-belt",
			"turbo-loader", "turbo-splitter", "turbo-transport-belt", "turbo-underground-belt", "underground-belt",
			"wooden-chest");

	public static final Set<String> BASE_TILES = ImmutableSet.of("artificial-jellynut-soil", "artificial-yumako-soil",
			"concrete", "foundation", "frozen-concrete", "frozen-hazard-concrete-left", "frozen-hazard-concrete-right",
			"frozen-refined-concrete", "frozen-refined-hazard-concrete-left", "frozen-refined-hazard-concrete-right",
			"hazard-concrete-left", "hazard-concrete-right", "ice-platform", "landfill", "overgrowth-jellynut-soil",
			"overgrowth-yumako-soil", "refined-concrete", "refined-hazard-concrete-left",
			"refined-hazard-concrete-right", "space-platform-foundation", "stone-path");

	public static final Map<String, String> RENDERING_MAP = ImmutableMap.<String, String>builder()//
			.put("ContainerRendering", "BasicContainerRendering")//
			.put("LoaderRendering", "Loader1x2Rendering")//
			.build();
    
    private final File folderMods;
    private final File fileModsModProfile;
    private final File fileModsModDownload;
    private final File fileModsModList;
    private final File fileModsModRendering;

    private final File folderData;
    private final File fileDataModRendering;
    private final File folderDataScriptOutput;
    private final File fileDataScriptOutputDumpZip;
    private final File folderDataAtlas;
    private final File fileDataAtlasManifestZip;

    public ProfileEditor(File folderMods, File folderData) {
        this.folderMods = folderMods;
        this.folderData = folderData;

        fileModsModProfile = new File(folderMods, "mod-profile.json");
        fileModsModDownload = new File(folderMods, "mod-download.json");
        fileModsModList = new File(folderMods, "mod-list.json");
        fileModsModRendering = new File(folderMods, "mod-rendering.json");

        fileDataModRendering = new File(folderData, "mod-rendering.json");
        folderDataScriptOutput = new File(folderData, "script-output");
        fileDataScriptOutputDumpZip = new File(folderDataScriptOutput, "data-raw-dump.zip");
        folderDataAtlas = new File(folderData, "atlas");
        fileDataAtlasManifestZip = new File(folderDataAtlas, "atlas-manifest.zip");
    }

    public File getFolderMods() {
        return folderMods;
    }

    public File getFolderData() {
        return folderData;
    }

    public static List<ProfileEditor> listProfiles() {
        JSONObject json = Config.get().getJSONObject("factorio_manager");
        File folderModsRoot = new File(json.optString("mods", "mods"));
        File folderDataRoot = new File(json.optString("data", "data"));

        List<ProfileEditor> profiles = new ArrayList<>();
        for (File file : folderModsRoot.listFiles()) {
            ProfileEditor profile = new ProfileEditor(file, new File(folderDataRoot, file.getName()));
            if (profile.hasModsConfig()) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    public boolean isValid() {
        return fileModsModProfile.exists();
    }

    public boolean needDownloadMods() {
        if (!fileModsModDownload.exists()) {
            return true;
        }

        try {
            JSONObject jsonModsModDownload = new JSONObject(Files.readString(fileModsModDownload.toPath()));
            for (String mod : jsonModsModDownload.keySet()) {
                String filename = jsonModsModDownload.getString(mod);
                //TODO merge download cached into download json so we can check if each zip file exists
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }

        return false;
    }

    public boolean hasModsConfig() {
        //TODO split these hasXXX() calls into staging order
        return fileModsModDownload.exists() && fileModsModList.exists() && fileModsModRendering.exists();
    }

    public boolean hasModsDownloaded() {
        for (File file : folderMods.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDataDump() {
        
        return fileDataScriptOutputDumpZip.exists();
    }

    public boolean hasCompleteData() {
        
        return hasDataDump() && fileDataAtlasManifestZip.exists() && fileDataModRendering.exists();
    }

    public static enum ProfileStatus {
        INVALID,
        DISABLED,
        STAGE_1_DOWNLOAD_MODS, // Mod Updates
        STAGE_2_FACTORIO_DUMP, // Factorio Updates
        STAGE_3_GENERATE_DATA, // Rendering Updates
        READY,
    }

    public ProfileStatus getStatus() {
        if (!isValid()) {
            return ProfileStatus.INVALID;
        } else if (!isEnabled()) {
            return ProfileStatus.DISABLED;
        } else if (!hasModsDownloaded()) {
            return ProfileStatus.NEED_DOWNLOAD;
        } else if (!hasDataDump()) {
            return ProfileStatus.NEED_DUMP;
        } else if (!hasCompleteData()) {
            return ProfileStatus.NEED_DATA;
        }
        return ProfileStatus.READY;
    }
    
    public boolean isEnabled() {
        if (!hasModsConfig()) {
            System.out.println("Profile does not have a valid mods configuration.");
            return false;
        }

        try {
            JSONObject jsonModsModProfile = new JSONObject(Files.readString(fileModsModProfile.toPath()));
            return jsonModsModProfile.optBoolean("enabled", true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setEnabled(boolean enabled) {
        if (!hasModsConfig()) {
            System.out.println("Profile does not have a valid mods configuration.");
            return;
        }

        try {
            JSONObject jsonModsModProfile = new JSONObject(Files.readString(fileModsModProfile.toPath()));
            jsonModsModProfile.put("enabled", enabled);
            try (FileWriter fw = new FileWriter(fileModsModProfile)) {
                jsonModsModProfile.write(fw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean createConfig(List<String> mods, boolean force) {

    }

    public boolean downloadMods(boolean force) {

    }

    public boolean factorioDump(boolean force) {

    }

    public boolean generateData(boolean force) {
        
    }

    public boolean generateProfile(String modGroup, boolean force, String... mods) {

        

        if (!force && hasModsConfig()) {
            System.out.println("Profile with name " + folderMods.getName() + " already exists.");
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
}
