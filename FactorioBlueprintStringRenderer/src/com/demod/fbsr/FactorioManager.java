package com.demod.fbsr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModLoader;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.EquipmentPrototype;
import com.demod.factorio.prototype.FluidPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TechPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.entity.UnknownEntityRendering;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class FactorioManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(FactorioManager.class);

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

	private static final List<EntityRendererFactory> entityFactories = new ArrayList<>();
	private static final List<TileRendererFactory> tileFactories = new ArrayList<>();
	private static final Map<String, EntityRendererFactory> entityFactoryByName = new HashMap<>();
	private static final Map<String, TileRendererFactory> tileFactoryByName = new HashMap<>();

	private static final List<ItemPrototype> items = new ArrayList<>();
	private static final List<RecipePrototype> recipes = new ArrayList<>();
	private static final List<FluidPrototype> fluids = new ArrayList<>();
	private static final List<TechPrototype> technologies = new ArrayList<>();
	private static final List<EntityPrototype> entities = new ArrayList<>();
	private static final List<TilePrototype> tiles = new ArrayList<>();
	private static final List<EquipmentPrototype> equipments = new ArrayList<>();

	private static final Map<String, ItemPrototype> itemByName = new HashMap<>();
	private static final Map<String, RecipePrototype> recipeByName = new HashMap<>();
	private static final Map<String, FluidPrototype> fluidByName = new HashMap<>();
	private static final Map<String, TechPrototype> technologyByName = new HashMap<>();
	private static final Map<String, EntityPrototype> entityByName = new HashMap<>();
	private static final Map<String, TilePrototype> tileByName = new HashMap<>();
	private static final Map<String, EquipmentPrototype> equipmentByName = new HashMap<>();

	private static final Cache<String, UnknownEntityRendering> unknownEntityFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();
	private static final Cache<String, UnknownTileRendering> unknownTileFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	public static List<FactorioData> getDatas() {
		return datas;
	}

	public static FactorioData getBaseData() {
		return baseData;
	}

	public static List<EntityPrototype> getEntities() {
		return entities;
	}

	public static List<EntityRendererFactory> getEntityFactories() {
		return entityFactories;
	}

	public static List<EquipmentPrototype> getEquipments() {
		return equipments;
	}

	public static List<FluidPrototype> getFluids() {
		return fluids;
	}

	public static List<ItemPrototype> getItems() {
		return items;
	}

	public static List<RecipePrototype> getRecipes() {
		return recipes;
	}

	public static List<TechPrototype> getTechnologies() {
		return technologies;
	}

	public static List<TilePrototype> getTiles() {
		return tiles;
	}

	public static List<TileRendererFactory> getTileFactories() {
		return tileFactories;
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

		baseData = entityFactories.stream().filter(e -> e.getGroupName().equals("Base")).map(e -> e.getData()).findAny()
				.orElseThrow(() -> new IOException("No entities for group \"Base\" was found."));
		entityFactories.forEach(e -> dataByGroupName.put(e.getGroupName(), e.getData()));

		// Place vanilla protos again to be the priority
		recipeByName.putAll(baseData.getTable().getRecipes());
		itemByName.putAll(baseData.getTable().getItems());
		fluidByName.putAll(baseData.getTable().getFluids());
		entityByName.putAll(baseData.getTable().getEntities());
		technologyByName.putAll(baseData.getTable().getTechnologies());
		tileByName.putAll(baseData.getTable().getTiles());
		equipmentByName.putAll(baseData.getTable().getEquipments());

		recipeByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(recipes::add);
		itemByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(items::add);
		fluidByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(fluids::add);
		entityByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(entities::add);
		technologyByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName))
				.forEach(technologies::add);
		tileByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(tiles::add);
		equipmentByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(equipments::add);
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
		LOGGER.info("MODS FOLDERS: {}", modsFiles.stream().map(f -> f.getName()).collect(Collectors.joining(", ")));

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

	public static EntityRendererFactory lookupEntityFactoryForName(String name) {
		return Optional.ofNullable(entityFactoryByName.get(name)).orElseGet(() -> {
			try {
				return unknownEntityFactories.get(name, () -> new UnknownEntityRendering(name));
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
		});
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
		return Optional.ofNullable(tileFactoryByName.get(name)).orElseGet(() -> {
			try {
				return unknownTileFactories.get(name, () -> new UnknownTileRendering(name));
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
		});
	}

	private static synchronized void registerEntityFactory(EntityRendererFactory factory) {
		String name = factory.getPrototype().getName();

		if (entityFactoryByName.containsKey(name)) {
			EntityRendererFactory existingFactory = entityFactoryByName.get(name);

			String detailMessage = String.format(
					"Entity '%s' is already registered in group '%s' from mod '%s'. Attempted re-registration from mod '%s' is not allowed.",
					name, existingFactory.getGroupName(), existingFactory.getData().folderMods.getName(),
					factory.getData().folderMods.getName());
			throw new IllegalArgumentException(detailMessage);
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
