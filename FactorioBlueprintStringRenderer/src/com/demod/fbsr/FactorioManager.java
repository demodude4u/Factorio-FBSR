package com.demod.fbsr;

import java.awt.image.BufferedImage;
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
import com.demod.factorio.prototype.AchievementPrototype;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.EquipmentPrototype;
import com.demod.factorio.prototype.FluidPrototype;
import com.demod.factorio.prototype.ItemGroupPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TechPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.entity.UnknownEntityRendering;
import com.demod.fbsr.fp.FPUtilitySprites;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class FactorioManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(FactorioManager.class);

	private static volatile boolean initializedPrototypes = false;
	private static volatile boolean initializedFactories = false;

	private static FactorioData baseData = null;
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
	private static final List<AchievementPrototype> achievements = new ArrayList<>();
	private static final List<ItemGroupPrototype> itemGroups = new ArrayList<>();

	private static final Map<String, ItemPrototype> itemByName = new HashMap<>();
	private static final Map<String, RecipePrototype> recipeByName = new HashMap<>();
	private static final Map<String, FluidPrototype> fluidByName = new HashMap<>();
	private static final Map<String, TechPrototype> technologyByName = new HashMap<>();
	private static final Map<String, EntityPrototype> entityByName = new HashMap<>();
	private static final Map<String, TilePrototype> tileByName = new HashMap<>();
	private static final Map<String, EquipmentPrototype> equipmentByName = new HashMap<>();
	private static final Map<String, AchievementPrototype> achievementByName = new HashMap<>();
	private static final Map<String, ItemGroupPrototype> itemGroupByName = new HashMap<>();

	private static FPUtilitySprites utilitySprites;

	private static final Cache<String, UnknownEntityRendering> unknownEntityFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();
	private static final Cache<String, UnknownTileRendering> unknownTileFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	private static File folderModsRoot;
	private static File folderDataRoot;

	private static boolean hasFactorioInstall;

	public static List<AchievementPrototype> getAchievements() {
		return achievements;
	}

	public static FactorioData getBaseData() {
		return baseData;
	}

	public static List<FactorioData> getDatas() {
		return datas;
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

	public static File getFolderDataRoot() {
		return folderDataRoot;
	}

	public static File getFolderModsRoot() {
		return folderModsRoot;
	}

	public static List<ItemPrototype> getItems() {
		return items;
	}

	public static List<ItemGroupPrototype> getItemGroups() {
		return itemGroups;
	}

	public static List<RecipePrototype> getRecipes() {
		return recipes;
	}

	public static List<TechPrototype> getTechnologies() {
		return technologies;
	}

	public static List<TileRendererFactory> getTileFactories() {
		return tileFactories;
	}

	public static List<TilePrototype> getTiles() {
		return tiles;
	}

	public static FPUtilitySprites getUtilitySprites() {
		return utilitySprites;
	}

	public static boolean hasFactorioInstall() {
		return hasFactorioInstall;
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

			if (jsonModRendering.getJSONObject("entities").has("Base")) {
				baseData = data;
			}

			EntityRendererFactory.registerFactories(FactorioManager::registerEntityFactory, data,
					jsonModRendering.getJSONObject("entities"));
			TileRendererFactory.registerFactories(FactorioManager::registerTileFactory, data,
					jsonModRendering.getJSONObject("tiles"));

		}

		if (baseData == null) {
			throw new IllegalStateException("No \"Base\" mod defined in any mod-rendering.json!");
		}

		utilitySprites = new FPUtilitySprites(baseData.getTable().getRaw("utility-sprites", "default").get());

		EntityRendererFactory.initFactories(entityFactories);
		TileRendererFactory.initFactories(tileFactories);

		entityFactories.forEach(e -> dataByGroupName.put(e.getGroupName(), e.getData()));

		// Place vanilla protos again to be the priority
		recipeByName.putAll(baseData.getTable().getRecipes());
		itemByName.putAll(baseData.getTable().getItems());
		fluidByName.putAll(baseData.getTable().getFluids());
		entityByName.putAll(baseData.getTable().getEntities());
		technologyByName.putAll(baseData.getTable().getTechnologies());
		tileByName.putAll(baseData.getTable().getTiles());
		equipmentByName.putAll(baseData.getTable().getEquipments());
		achievementByName.putAll(baseData.getTable().getAchievements());
		itemGroupByName.putAll(baseData.getTable().getItemGroups());

		recipeByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(recipes::add);
		itemByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(items::add);
		fluidByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(fluids::add);
		entityByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(entities::add);
		technologyByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName))
				.forEach(technologies::add);
		tileByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(tiles::add);
		equipmentByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(equipments::add);
		achievementByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName))
				.forEach(achievements::add);
		itemGroupByName.values().stream().sorted(Comparator.comparing(DataPrototype::getName)).forEach(itemGroups::add);
	}

	public static void initializePrototypes() throws JSONException, IOException {
		if (initializedPrototypes) {
			throw new IllegalStateException("Already Initialized Prototypes!");
		}
		initializedPrototypes = true;

		JSONObject json = Config.get().getJSONObject("factorio_manager");

		hasFactorioInstall = json.has("install") && json.has("executable");
		Optional<String> factorioData = Optional.ofNullable(json.optString("install", null));
		Optional<String> factorioExecutable = Optional.ofNullable(json.optString("executable", null));

		folderModsRoot = new File(json.optString("mods", "mods"));
		if (folderModsRoot.mkdirs()) {
			File folderModsVanilla = new File(folderModsRoot, "mods-vanilla");
			folderModsVanilla.mkdir();
		}

		folderDataRoot = new File(json.optString("data", "data"));
		folderDataRoot.mkdirs();

		boolean modPortalApi;
		String modPortalApiUsername;
		String modPortalApiPassword;
		if (json.has("mod_portal_api")) {
			JSONObject jsonModPortalAPI = json.getJSONObject("mod_portal_api");
			if (jsonModPortalAPI.has("username") && jsonModPortalAPI.has("password")) {
				modPortalApi = true;
				modPortalApiUsername = jsonModPortalAPI.getString("username");
				modPortalApiPassword = jsonModPortalAPI.getString("password");
			} else {
				modPortalApi = false;
				modPortalApiUsername = null;
				modPortalApiPassword = null;
			}
		} else {
			modPortalApi = false;
			modPortalApiUsername = null;
			modPortalApiPassword = null;
		}

		List<String> mods;
		if (json.has("mods_include")) {
			JSONArray jsonModsInclude = json.getJSONArray("mods_include");
			mods = IntStream.range(0, jsonModsInclude.length()).mapToObj(i -> jsonModsInclude.getString(i))
					.collect(Collectors.toList());
			if (hasFactorioInstall) {
				mods = mods.stream().filter(n -> new File(folderModsRoot, n).exists()).collect(Collectors.toList());
			} else {
				mods = mods.stream().filter(n -> new File(folderDataRoot, n).exists()).collect(Collectors.toList());
			}
		} else {
			mods = Arrays.asList(folderModsRoot.listFiles()).stream()
					.filter(f -> f.isDirectory() && new File(f, "mod-rendering.json").exists()).map(f -> f.getName())
					.collect(Collectors.toList());
		}
		LOGGER.info("MODS FOLDERS: {}", mods.stream().collect(Collectors.joining(", ")));

		mods.parallelStream().peek(name -> {
			try {
				File folderMods = new File(folderModsRoot, name);
				File folderData = new File(folderDataRoot, name);
				folderData.mkdir();

				if (hasFactorioInstall) {

					File fileModDownloadCached = new File(folderMods, "mod-download-cached.json");
					boolean cacheChange = false;
					JSONObject jsonModDownloadCached;
					if (fileModDownloadCached.exists()) {
						jsonModDownloadCached = new JSONObject(Files.readString(fileModDownloadCached.toPath()));
					} else {
						jsonModDownloadCached = new JSONObject();
						cacheChange = true;
					}

					File fileModDownload = new File(folderMods, "mod-download.json");
					if (modPortalApi && fileModDownload.exists()) {
						JSONObject jsonModDownload = new JSONObject(Files.readString(fileModDownload.toPath()));
						boolean auth = false;
						String authParams = null;
						for (String modName : jsonModDownload.keySet()) {
							String modVersion = jsonModDownload.getString(modName);

							if (!jsonModDownloadCached.has(modName)) {
								jsonModDownloadCached.put(modName, new JSONObject());
								cacheChange = true;
							}
							JSONObject jsonModCached = jsonModDownloadCached.getJSONObject(modName);
							if (!jsonModCached.has(modVersion)) {
								jsonModCached.put(modVersion,
										FactorioModPortal.findModReleaseInfo(modName, modVersion));
								cacheChange = true;
							}
							JSONObject jsonRelease = jsonModCached.getJSONObject(modVersion);

							File fileModZip = new File(folderMods, jsonRelease.getString("file_name"));
							if (!fileModZip.exists()) {
								if (!auth) {
									auth = true;
									authParams = FactorioModPortal.getAuthParams(modPortalApiUsername,
											modPortalApiPassword);
								}
								FactorioModPortal.downloadMod(folderMods, modName, modVersion, authParams);
							}
						}

					}

					if (cacheChange) {
						Files.writeString(fileModDownloadCached.toPath(), jsonModDownloadCached.toString(2));
						LOGGER.info("WRITE MOD DOWNLOAD CACHED: {}", fileModDownloadCached.getAbsolutePath());
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

		}).parallel().map(name -> {
			try {
				JSONObject fdConfig = new JSONObject();
				File folderMods = new File(folderModsRoot, name);
				File folderData = new File(folderDataRoot, name);
				folderData.mkdir();
				fdConfig.put("mods", folderMods.getAbsolutePath());
				fdConfig.put("data", folderData.getAbsolutePath());

				if (hasFactorioInstall) {
					fdConfig.put("factorio", factorioData.get());
					fdConfig.put("executable", factorioExecutable.get());
				}

				FactorioData data = new FactorioData(fdConfig);
				data.initialize(false);
				return data;

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}

		}).sequential().forEach(data -> {
			datas.add(data);

			data.getMods().stream().forEach(s -> dataByModName.put(s, data));

			recipeByName.putAll(data.getTable().getRecipes());
			itemByName.putAll(data.getTable().getItems());
			fluidByName.putAll(data.getTable().getFluids());
			entityByName.putAll(data.getTable().getEntities());
			technologyByName.putAll(data.getTable().getTechnologies());
			tileByName.putAll(data.getTable().getTiles());
			equipmentByName.putAll(data.getTable().getEquipments());
			achievementByName.putAll(data.getTable().getAchievements());
			itemGroupByName.putAll(data.getTable().getItemGroups());
		});
	}

	public static Optional<AchievementPrototype> lookupAchievementByName(String name) {
		return Optional.ofNullable(achievementByName.get(name));
	}

	public static FactorioData lookupDataByGroupName(String groupName) {
		return dataByGroupName.get(groupName);
	}

	public static List<FactorioData> lookupDataByModName(String modName) {
		return dataByModName.get(modName);
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

	public static Optional<ItemGroupPrototype> lookupItemGroupByName(String name) {
		return Optional.ofNullable(itemGroupByName.get(name));
	}

	public static BufferedImage lookupModImage(String filename) {
		if (!hasFactorioInstall) {
			LOGGER.error("FACTORIO INSTALL NEEDED -- LOAD IMAGES!");
			System.exit(-1);
			return null;
		}

		try {
			String firstSegment = filename.split("\\/")[0];
			String modName = firstSegment.substring(2, firstSegment.length() - 2);
			return dataByModName.get(modName).get(0).getModImage(filename);
		} catch (Exception e) {
			LOGGER.error("FILENAME: {}", filename);
			throw e;
		}
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
