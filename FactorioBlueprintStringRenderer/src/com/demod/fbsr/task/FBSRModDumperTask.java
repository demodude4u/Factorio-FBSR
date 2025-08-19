package com.demod.fbsr.task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.FactorioModPortal;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class FBSRModDumperTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(FBSRModDumperTask.class);

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

	public static void main(String[] args) throws IOException {
		String[] mods = { "cybersyn-combinator" };

		JSONObject cfgFactorioManager = Config.get().getJSONObject("factorio_manager");
		JSONObject cfgModPortalApi = cfgFactorioManager.getJSONObject("mod_portal_api");

		String factorioInstall = cfgFactorioManager.getString("install");
		String factorioExecutable = cfgFactorioManager.getString("executable");
		String username = cfgModPortalApi.getString("username");
		String password = cfgModPortalApi.getString("password");

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

		File folderMods = new File("tempmods");
		folderMods.mkdirs();

		boolean auth = false;
		String authParams = null;

		Map<String, String> modVersions = new LinkedHashMap<>();
		Set<String> modZipFilenames = new HashSet<>();
		for (String mod : allMods) {
			if (BUILTIN_MODS.contains(mod)) {
				continue;
			}
			JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfo(mod);
			String version = jsonRelease.getString("version");
			modVersions.put(mod, version);
			String filename = jsonRelease.getString("file_name");
			File fileModZip = new File(folderMods, filename);
			if (!fileModZip.exists()) {
				if (!auth) {
					auth = true;
					authParams = FactorioModPortal.getAuthParams(username, password);
				}
				FactorioModPortal.downloadMod(folderMods, mod, version, authParams);
			}
			modZipFilenames.add(filename);
		}

		for (File file : folderMods.listFiles()) {
			if (file.isDirectory()) {
				continue;
			}
			if (file.getName().endsWith("zip") && !modZipFilenames.contains(file.getName())) {
				file.delete();
				LOGGER.info("Deleted " + file.getAbsolutePath());
			}
		}

		JSONObject jsonModList = new JSONObject();
		JSONArray jsonModListMods = new JSONArray();
		jsonModList.put("mods", jsonModListMods);
		for (String mod : allMods) {
			JSONObject jsonMod = new JSONObject();
			jsonMod.put("name", mod);
			jsonMod.put("enabled", true);
			jsonModListMods.put(jsonMod);
		}
		for (String mod : BUILTIN_MODS) {
			if (allMods.contains(mod)) {
				continue;
			}
			JSONObject jsonMod = new JSONObject();
			jsonMod.put("name", mod);
			jsonMod.put("enabled", false);
			jsonModListMods.put(jsonMod);
		}
		File fileModList = new File(folderMods, "mod-list.json");
		if (fileModList.exists()) {
			fileModList.setWritable(true);
		}
		try (FileWriter fw = new FileWriter(fileModList)) {
			jsonModList.write(fw);
		}

		File folderData = new File("tempdata");
		folderData.deleteOnExit();
		JSONObject cfgFactorioData = new JSONObject();
		cfgFactorioData.put("factorio", factorioInstall);
		cfgFactorioData.put("executable", factorioExecutable);
		cfgFactorioData.put("data", folderData.getAbsolutePath());
		cfgFactorioData.put("mods", folderMods.getAbsolutePath());
		FactorioData factorioData = new FactorioData(cfgFactorioData);
		factorioData.initialize(false);
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
					}
				});

		table.getTiles().values().stream().filter(t -> isBlueprintable(t))
				.sorted(Comparator.comparing(t -> t.getName())).forEach(t -> {
					if (!BASE_TILES.contains(t.getName())) {
					}
				});

		// mod-list
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			for (String mod : allMods) {
				JSONObject json = new JSONObject();
				Utils.terribleHackToHaveOrderedJSONObject(json);
				json.put("name", mod);
				json.put("enabled", true);
				pw.println(json.toString(2));
				pw.println();
			}
			pw.flush();
			LOGGER.info("mod-list.json additions:\n" + sw.toString());
		}

		// mod-download
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			JSONObject json = new JSONObject();
			Utils.terribleHackToHaveOrderedJSONObject(json);
			for (String mod : allMods) {
				json.put(mod, modVersions.get(mod));
			}
			pw.println(json.toString(4));
			pw.flush();
			LOGGER.info("mod-download.json additions:\n" + sw.toString());
		}

		// mod-rendering
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			JSONObject json = new JSONObject();
			Utils.terribleHackToHaveOrderedJSONObject(json);
			for (Entry<String, String> entry : modEntityRenderings.entrySet()) {
				json.put(entry.getKey(), entry.getValue());
			}
			pw.println(json.toString(4));
			pw.flush();
			LOGGER.info("mod-rendering.json additions:\n" + sw.toString());
		}

//		table.getItems().values().stream().filter(i -> !i.lua().get("place_result").isnil())
//				.map(i -> i.lua().get("place_result").tojstring()).sorted()
//				.forEach(s -> System.out.println("\"" + s + "\","));
//		System.out.println();
//		table.getItems().values().stream().filter(i -> !i.lua().get("place_as_tile").isnil())
//				.map(i -> i.lua().get("place_as_tile").get("result").tojstring()).sorted()
//				.forEach(s -> System.out.println("\"" + s + "\","));
	}

	private static void walkDependencies(Set<String> allMods, String mod) throws IOException {
		JSONObject jsonRelease = FactorioModPortal.findLatestModReleaseInfoFull(mod);
		JSONArray jsonDependencies = jsonRelease.getJSONObject("info_json").getJSONArray("dependencies");

		for (int i = 0; i < jsonDependencies.length(); i++) {
			String[] split = jsonDependencies.getString(i).split("\\s+");
			String depSymbol = "";
			String depMod = null;
//			String depCompare = "";
//			String depVersion = "";
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
//				depCompare = split[1];
//				depVersion = split[2];
				break;
			case 4:
				depSymbol = split[0];
				depMod = split[1];
//				depCompare = split[2];
//				depVersion = split[3];
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

}
