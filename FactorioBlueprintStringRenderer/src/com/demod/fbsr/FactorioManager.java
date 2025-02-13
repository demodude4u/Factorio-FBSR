package com.demod.fbsr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModLoader;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.EquipmentPrototype;
import com.demod.factorio.prototype.FluidPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TechPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.bs.BSEntity;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class FactorioManager {

	public static class LookupDataRawResult {
		public final FactorioData data;
		public final LuaValue value;

		public LookupDataRawResult(FactorioData data, LuaValue value) {
			this.data = data;
			this.value = value;
		}
	}

	private static volatile boolean initializedPrototypes = false;
	private static volatile boolean initializedFactories = false;

	private static FactorioData baseData;
	private static final List<FactorioData> datas = new ArrayList<>();
	private static final ListMultimap<String, FactorioData> dataByModName = ArrayListMultimap.create();
	private static final Map<String, FactorioData> dataByGroupName = new HashMap<>();

	@SuppressWarnings("rawtypes")
	private static final List<EntityRendererFactory> entityFactories = new ArrayList<>();
	@SuppressWarnings("rawtypes")
	private static final Map<String, EntityRendererFactory> entityFactoryByName = new HashMap<>();

	private static final List<TileRendererFactory> tileFactories = new ArrayList<>();
	private static final Map<String, TileRendererFactory> tileFactoryByName = new HashMap<>();

	// TODO prioritize vanilla first
	private static final Map<String, ItemPrototype> itemByName = new HashMap<>();
	private static final Map<String, RecipePrototype> recipeByName = new HashMap<>();
	private static final Map<String, FluidPrototype> fluidByName = new HashMap<>();
	private static final Map<String, TechPrototype> technologyByName = new HashMap<>();
	private static final Map<String, EntityPrototype> entityByName = new HashMap<>();
	private static final Map<String, TilePrototype> tileByName = new HashMap<>();
	private static final Map<String, EquipmentPrototype> equipmentByName = new HashMap<>();

	public static List<FactorioData> getDatas() {
		return datas;
	}

	public static FactorioData getBaseData() {
		return baseData;
	}

	public static Map<String, EntityPrototype> getEntities() {
		return entityByName;
	}

	public static Map<String, EquipmentPrototype> getEquipments() {
		return equipmentByName;
	}

	public static Map<String, FluidPrototype> getFluids() {
		return fluidByName;
	}

	public static Map<String, ItemPrototype> getItems() {
		return itemByName;
	}

	public static Map<String, RecipePrototype> getRecipes() {
		return recipeByName;
	}

	public static Map<String, TechPrototype> getTechnologies() {
		return technologyByName;
	}

	public static Map<String, TilePrototype> getTiles() {
		return tileByName;
	}

	public static void initializeFactories() throws JSONException, IOException {
		if (!initializedPrototypes) {
			throw new IllegalStateException("Must initialize prototypes first!");
		}
		if (initializedFactories) {
			throw new IllegalStateException("Already Initialized Factories!");
		}
		initializedFactories = true;

		for (FactorioData data : datas) {
			File folderMods = data.folderMods;

			JSONObject jsonModRendering = new JSONObject(
					Files.readString(new File(folderMods, "mod-rendering.json").toPath()));
			EntityRendererFactory.registerFactories(FactorioManager::registerEntityFactory, data,
					jsonModRendering.getJSONObject("entities"));
			TileRendererFactory.registerFactories(FactorioManager::registerTileFactory, data,
					jsonModRendering.getJSONObject("tiles"));

		}

		EntityRendererFactory.initFactories(entityFactories);
		TileRendererFactory.initFactories(tileFactories);

		baseData = entityFactories.stream().filter(e -> e.getGroupName().equals("Base")).map(e -> e.getData())
				.findAny().orElseThrow(() -> new IOException("No entities for group \"Base\" was found."));
		entityFactories.forEach(e -> dataByGroupName.put(e.getGroupName(), e.getData()));

		// Place vanilla protos again to be the priority
		recipeByName.putAll(baseData.getTable().getRecipes());
		itemByName.putAll(baseData.getTable().getItems());
		fluidByName.putAll(baseData.getTable().getFluids());
		entityByName.putAll(baseData.getTable().getEntities());
		technologyByName.putAll(baseData.getTable().getTechnologies());
		tileByName.putAll(baseData.getTable().getTiles());
		equipmentByName.putAll(baseData.getTable().getEquipments());
	}

	public static void initializePrototypes() throws JSONException, IOException {
		if (initializedPrototypes) {
			throw new IllegalStateException("Already Initialized Prototypes!");
		}
		initializedPrototypes = true;

		JSONObject json = Config.get().getJSONObject("factorio_manager");

		String factorio = json.getString("install");

		File folderModsRoot = new File(json.optString("mods", "mods"));
		if (folderModsRoot.mkdirs()) {
			File folderModsVanilla = new File(folderModsRoot, "mods-vanilla");
			folderModsVanilla.mkdir();
		}

		File folderDataRoot = new File(json.optString("data", "data"));
		folderDataRoot.mkdirs();

		boolean modPortalApi;
		String modPortalApiUsername = null;
		String modPortalApiPassword = null;
		if (json.has("mod_portal_api")) {
			JSONObject jsonModPortalAPI = json.getJSONObject("mod_portal_api");
			if (!jsonModPortalAPI.has("username") || !jsonModPortalAPI.has("password")//
					|| (modPortalApiUsername = jsonModPortalAPI.getString("username")).isBlank() //
					|| (modPortalApiPassword = jsonModPortalAPI.getString("password")).isBlank()) {
				modPortalApi = false;
			} else {
				modPortalApi = true;
			}
		} else {
			modPortalApi = false;
		}

		List<File> modsFiles;
		if (json.has("mods_include")) {
			JSONArray jsonModsInclude = json.getJSONArray("mods_include");
			modsFiles = IntStream.range(0, jsonModsInclude.length())
					.mapToObj(i -> new File(folderModsRoot, jsonModsInclude.getString(i))).filter(File::exists)
					.collect(Collectors.toList());
		} else {
			modsFiles = Arrays.asList(folderModsRoot.listFiles());
		}
		System.out
				.println("MODS FOLDERS: " + modsFiles.stream().map(f -> f.getName()).collect(Collectors.joining(", ")));

		for (File folderMods : modsFiles) {
			if (!folderMods.exists() || !folderMods.isDirectory()) {
				continue;
			}
			if (!new File(folderMods, "mod-list.json").exists()) {
				continue;
			}

			File fileModRendering = new File(folderMods, "mod-download.json");
			if (modPortalApi && fileModRendering.exists()) {
				JSONObject jsonModDownload = new JSONObject(Files.readString(fileModRendering.toPath()));
				boolean auth = false;
				String authParams = null;
				for (String modName : jsonModDownload.keySet()) {
					String modVersion = jsonModDownload.getString(modName);
					JSONObject jsonRelease = FactorioModPortal.findModReleaseInfo(modName, modVersion);
					File fileModZip = new File(folderMods, jsonRelease.getString("file_name"));
					if (!fileModZip.exists()) {
						if (!auth) {
							auth = true;
							authParams = FactorioModPortal.getAuthParams(modPortalApiUsername, modPortalApiPassword);
						}
						FactorioModPortal.downloadMod(folderMods, modName, modVersion, authParams);
					}
				}

			}

			File folderData = new File(folderDataRoot, folderMods.getName());
			folderData.mkdir();

			JSONObject fdConfig = new JSONObject();
			fdConfig.put("factorio", factorio);
			fdConfig.put("mods", folderMods.getAbsolutePath());
			fdConfig.put("data", folderData.getAbsolutePath());

			FactorioData data = new FactorioData(fdConfig);
			data.initialize();
			datas.add(data);

			ModLoader modLoader = data.getModLoader();
			modLoader.getMods().keySet().stream().forEach(s -> dataByModName.put(s, data));

			recipeByName.putAll(data.getTable().getRecipes());
			itemByName.putAll(data.getTable().getItems());
			fluidByName.putAll(data.getTable().getFluids());
			entityByName.putAll(data.getTable().getEntities());
			technologyByName.putAll(data.getTable().getTechnologies());
			tileByName.putAll(data.getTable().getTiles());
			equipmentByName.putAll(data.getTable().getEquipments());
		}
	}

	public static FactorioData lookupDataByGroupName(String groupName) {
		return dataByGroupName.get(groupName);
	}

	public static List<FactorioData> lookupDataByModName(String modName) {
		return dataByModName.get(modName);
	}

	public static Optional<LookupDataRawResult> lookupDataRaw(String[] path, String key) {
		for (FactorioData data : datas) {
			Optional<LuaValue> luaParent = data.getTable().getRaw(path);
			if (!luaParent.isPresent() || luaParent.get().isnil()) {
				continue;
			}
			LuaValue luaValue = luaParent.get().get(key);
			if (luaValue.isnil()) {
				continue;
			}
			return Optional.of(new LookupDataRawResult(data, luaValue));
		}
		return Optional.empty();
	}

	public static Optional<EntityPrototype> lookupEntityByName(String name) {
		return Optional.ofNullable(entityByName.get(name));
	}

	@SuppressWarnings("unchecked")
	public static <E extends BSEntity> EntityRendererFactory<E> lookupEntityFactoryForName(String name) {
		return Optional.ofNullable(entityFactoryByName.get(name)).orElse(EntityRendererFactory.UNKNOWN);
	}

	public static Optional<EquipmentPrototype> lookupEquipmentByName(String name) {
		return Optional.ofNullable(equipmentByName.get(name));
	}

	public static Optional<FluidPrototype> lookupFluidByName(String name) {
		return Optional.ofNullable(fluidByName.get(name));
	}

	public static Optional<ItemPrototype> lookupItemByName(String name) {
		return Optional.ofNullable(itemByName.get(name));
	}

	public static Optional<RecipePrototype> lookupRecipeByName(String name) {
		return Optional.ofNullable(recipeByName.get(name));
	}

	public static Optional<TechPrototype> lookupTechnologyByName(String name) {
		return Optional.ofNullable(technologyByName.get(name));
	}

	public static Optional<TilePrototype> lookupTileByName(String name) {
		return Optional.ofNullable(tileByName.get(name));
	}

	public static TileRendererFactory lookupTileFactoryForName(String name) {
		return Optional.ofNullable(tileFactoryByName.get(name)).orElse(TileRendererFactory.UNKNOWN);
	}

	@SuppressWarnings("rawtypes")
	private static synchronized void registerEntityFactory(EntityRendererFactory factory) {
		String name = factory.getPrototype().getName();
		if (entityFactoryByName.containsKey(name)) {
			throw new IllegalArgumentException("Entity already exists! " + name);
		}
		entityFactories.add(factory);
		entityFactoryByName.put(name, factory);
	}

	private static synchronized void registerTileFactory(TileRendererFactory factory) {
		String name = factory.getPrototype().getName();
		if (tileFactoryByName.containsKey(name)) {
			throw new IllegalArgumentException("Tile already exists! " + name);
		}
		tileFactories.add(factory);
		tileFactoryByName.put(name, factory);
	}
}
