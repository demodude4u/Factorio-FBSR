package com.demod.fbsr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.cli.CmdBot;
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

    private final File folderModsRoot;
    
    private File folderProfile = null;

    public ProfileEditor() {
        JSONObject json = Config.get().getJSONObject("factorio_manager");
        folderModsRoot = new File(json.optString("mods", "mods"));
    }

    public File getProfile() {
        return folderProfile;
    }

    public void setProfile(File folderProfile) {
        this.folderProfile = folderProfile;
    }

    public List<String> listProfileNames() {
        List<String> profileNames = new ArrayList<>();
        File[] files = folderModsRoot.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isValidProfileFolder(file)) {
                    profileNames.add(file.getName());
                }
            }
        }
        return profileNames;
    }

    public boolean findProfile(String name) {
        File file = new File(folderModsRoot, name);
        if (isValidProfileFolder(file)) {
            folderProfile = file;
            return true;
        }
        return false;
    }

    private boolean isValidProfileFolder(File file) {
        return file.exists() && file.isDirectory() && new File(file, "mod-rendering.json").exists();
    }
    
    public boolean generateProfile(String name, String modGroup, boolean force, String... mods) {
        File file = new File(folderModsRoot, name);
        if (!force && isValidProfileFolder(file)) {
            LOGGER.warn("Profile with name '{}' already exists.", name);
            return false;
        }

        JSONObject cfgFactorioManager = Config.get().getJSONObject("factorio_manager");

        File folderTemp = new File(cfgFactorioManager.optString("temp", "temp"));
        folderTemp.mkdirs();
        File folderTempMods = new File(folderTemp, "mods");
        File folderTempData = new File(folderTemp, "data");
        folderTempMods.mkdir();
        folderTempData.mkdir();

        Set<String> allMods = new LinkedHashSet<>();
        Collections.addAll(allMods, mods);
		try {
            for (String mod : mods) {
            	if (BUILTIN_MODS.contains(mod)) {
            		continue;
            	}
            	walkDependencies(allMods, mod);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        LOGGER.info("Requested Mods: " + Arrays.asList(mods).stream().collect(Collectors.joining(", ")));
		LOGGER.info("");
		LOGGER.info("Mod List: " + allMods.stream().collect(Collectors.joining(", ")));
		LOGGER.info("");

        JSONObject jsonModDownload = new JSONObject();
		try {
            for (String mod : allMods) {
            	if (BUILTIN_MODS.contains(mod)) {
            		continue;
            	}
            	JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfo(mod);
            	String version = jsonRelease.getString("version");
            	jsonModDownload.put(mod, version);
                LOGGER.info("MOD DOWNLOAD - {}: {}", mod, version);
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
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

        File fileTempModDownload = new File(folderTempMods, "mod-download.json");
		if (fileTempModDownload.exists()) {
			fileTempModDownload.setWritable(true);
		}
		try (FileWriter fw = new FileWriter(fileTempModDownload)) {
			jsonModDownload.write(fw);
		} catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            FactorioManager.downloadMods(folderTempMods);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        File fileTempModList = new File(folderTempMods, "mod-list.json");
		if (fileTempModList.exists()) {
			fileTempModList.setWritable(true);
		}
		try (FileWriter fw = new FileWriter(fileTempModList)) {
			jsonModList.write(fw);
		} catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        JSONObject fdConfig = new JSONObject();
        folderTempData.mkdir();
        fdConfig.put("mods", folderTempMods.getAbsolutePath());
        fdConfig.put("data", folderTempData.getAbsolutePath());

        String factorioInstall = cfgFactorioManager.getString("install");
		String factorioExecutable = cfgFactorioManager.getString("executable");
        
        JSONObject cfgFactorioData = new JSONObject();
		cfgFactorioData.put("factorio", factorioInstall);
		cfgFactorioData.put("executable", factorioExecutable);
		cfgFactorioData.put("data", folderTempData.getAbsolutePath());
		cfgFactorioData.put("mods", folderTempMods.getAbsolutePath());
		FactorioData factorioData = new FactorioData(cfgFactorioData);
		try {
            factorioData.initialize(false);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
		DataTable table = factorioData.getTable();

        Map<String, String> modEntityRenderings = new LinkedHashMap<String, String>();
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
						modEntityRenderings.put(e.getName(), rendering);
                        LOGGER.info("MOD ENTITY - {}: {}", e.getName(), rendering);
					}
				});

        List<String> modTiles = new ArrayList<>();
		table.getTiles().values().stream().filter(t -> isBlueprintable(t))
				.sorted(Comparator.comparing(t -> t.getName())).forEach(t -> {
					if (!BASE_TILES.contains(t.getName())) {
                        modTiles.add(t.getName());
                        LOGGER.info("MOD TILE - {}", t.getName());
					}
				});

        JSONObject jsonModRendering = new JSONObject();
        JSONObject jsonModRenderingEntities = new JSONObject();
        jsonModRendering.put("entities", jsonModRenderingEntities);
        JSONObject jsonModRenderingEntitiesModGroup = new JSONObject();
        jsonModRenderingEntities.put(modGroup, jsonModRenderingEntitiesModGroup);
        for (Map.Entry<String, String> entry : modEntityRenderings.entrySet()) {
            jsonModRenderingEntitiesModGroup.put(entry.getKey(), entry.getValue());
        }
        JSONObject jsonModRenderingTiles = new JSONObject();
        jsonModRendering.put("tiles", jsonModRenderingTiles);
        JSONArray jsonModRenderingTilesModGroup = new JSONArray();
        jsonModRenderingTiles.put(modGroup, jsonModRenderingTilesModGroup);
        for (String tile : modTiles) {
            jsonModRenderingTilesModGroup.put(tile);
        }

        File fileTempModRendering = new File(folderTempMods, "mod-rendering.json");
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
        File fileModDownload = new File(folderMods, fileTempModDownload.getName());
        File fileModList = new File(folderMods, fileTempModList.getName());
        File fileModRendering = new File(folderMods, fileTempModRendering.getName());
        
        try {
            folderMods.mkdirs();
            Files.copy(fileTempModDownload.toPath(), fileModDownload.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(fileTempModList.toPath(), fileModList.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(fileTempModRendering.toPath(), fileModRendering.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();

            fileModDownload.delete();
            fileModList.delete();
            fileModRendering.delete();
            folderMods.delete();
            return false;
        }

        this.folderProfile = folderMods;
        return true;
    }

    private static void walkDependencies(Set<String> allMods, String mod) throws IOException {
		JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfoFull(mod);
		JSONArray jsonDependencies = jsonRelease.getJSONObject("info_json").getJSONArray("dependencies");

		for (int i = 0; i < jsonDependencies.length(); i++) {
			String[] split = jsonDependencies.getString(i).split("\\s+");
			String depSymbol = "";
			String depMod = null;
			switch (split.length) {
			case 1:
				depMod = split[0];
				break;
			case 2:
				depSymbol = split[0];
				depMod = split[1];
				break;
			case 3:
				depMod = split[0];
				break;
			case 4:
				depSymbol = split[0];
				depMod = split[1];
				break;
			}
			if (!depSymbol.isBlank()) {
				continue;
			}
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
}
