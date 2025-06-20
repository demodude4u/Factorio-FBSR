package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import org.rapidoid.data.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
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
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.entity.UnknownEntityRendering;
import com.demod.fbsr.fp.FPUtilitySprites;
import com.demod.fbsr.gui.GUIStyle;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class FactorioManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(FactorioManager.class);

	private static boolean hasFactorioInstall;
	private static File factorioInstall;
	private static File factorioExecutable;
	private static String factorioVersion;

	private static boolean hasModPortalApi;
	private static String modPortalApiUsername;
	private static String modPortalApiPassword;


	static {
		JSONObject json = Config.get().getJSONObject("factorio_manager");
		
		if (json.has("install") && json.has("executable")) {
			hasFactorioInstall = true;
			factorioInstall = new File(json.getString("install"));
			factorioExecutable = new File(json.getString("executable"));
			factorioVersion = FactorioData.getVersionFromExecutable(factorioExecutable).get();
		} else {
			hasFactorioInstall = false;
			factorioInstall = null;
			factorioExecutable = null;
			factorioVersion = null;
		}

		if (json.has("mod_portal_api")) {
			JSONObject jsonModPortalAPI = json.getJSONObject("mod_portal_api");
			if (jsonModPortalAPI.has("username") && jsonModPortalAPI.has("password")) {
				hasModPortalApi = true;
				modPortalApiUsername = jsonModPortalAPI.getString("username");
				modPortalApiPassword = jsonModPortalAPI.getString("password");
			} else {
				hasModPortalApi = false;
				modPortalApiUsername = null;
				modPortalApiPassword = null;
			}
		} else {
			hasModPortalApi = false;
			modPortalApiUsername = null;
			modPortalApiPassword = null;
		}
	}

	private volatile boolean initializedPrototypes = false;
	private volatile boolean initializedFactories = false;

	private Profile profileVanilla = null;
	private final List<Profile> profiles;

	private final Map<FactorioData, Profile> profileByData = new HashMap<>();
	private final Map<String, Profile> profileByGroupName = new HashMap<>();
	private final ListMultimap<String, Profile> profileByModName = ArrayListMultimap.create();

	private final List<EntityRendererFactory> entityFactories = new ArrayList<>();
	private final List<TileRendererFactory> tileFactories = new ArrayList<>();
	private final Map<String, EntityRendererFactory> entityFactoryByName = new HashMap<>();
	private final Map<String, TileRendererFactory> tileFactoryByName = new HashMap<>();

	private final List<ItemPrototype> items = new ArrayList<>();
	private final List<RecipePrototype> recipes = new ArrayList<>();
	private final List<FluidPrototype> fluids = new ArrayList<>();
	private final List<TechPrototype> technologies = new ArrayList<>();
	private final List<EntityPrototype> entities = new ArrayList<>();
	private final List<TilePrototype> tiles = new ArrayList<>();
	private final List<EquipmentPrototype> equipments = new ArrayList<>();
	private final List<AchievementPrototype> achievements = new ArrayList<>();
	private final List<ItemGroupPrototype> itemGroups = new ArrayList<>();

	private final Map<String, ItemPrototype> itemByName = new HashMap<>();
	private final Map<String, RecipePrototype> recipeByName = new HashMap<>();
	private final Map<String, FluidPrototype> fluidByName = new HashMap<>();
	private final Map<String, TechPrototype> technologyByName = new HashMap<>();
	private final Map<String, EntityPrototype> entityByName = new HashMap<>();
	private final Map<String, TilePrototype> tileByName = new HashMap<>();
	private final Map<String, EquipmentPrototype> equipmentByName = new HashMap<>();
	private final Map<String, AchievementPrototype> achievementByName = new HashMap<>();
	private final Map<String, ItemGroupPrototype> itemGroupByName = new HashMap<>();

	private FPUtilitySprites utilitySprites;

	private final Cache<String, UnknownEntityRendering> unknownEntityFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();
	private final Cache<String, UnknownTileRendering> unknownTileFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	public FactorioManager(List<Profile> profiles) {
		this.profiles = profiles;
	}

	public List<AchievementPrototype> getAchievements() {
		return achievements;
	}

	public Profile getProfileVanilla() {
		return profileVanilla;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public List<EntityPrototype> getEntities() {
		return entities;
	}

	public List<EntityRendererFactory> getEntityFactories() {
		return entityFactories;
	}

	public List<EquipmentPrototype> getEquipments() {
		return equipments;
	}

	public List<FluidPrototype> getFluids() {
		return fluids;
	}

	public List<ItemPrototype> getItems() {
		return items;
	}

	public List<ItemGroupPrototype> getItemGroups() {
		return itemGroups;
	}

	public List<RecipePrototype> getRecipes() {
		return recipes;
	}

	public List<TechPrototype> getTechnologies() {
		return technologies;
	}

	public List<TileRendererFactory> getTileFactories() {
		return tileFactories;
	}

	public List<TilePrototype> getTiles() {
		return tiles;
	}

	public FPUtilitySprites getUtilitySprites() {
		return utilitySprites;
	}

	public static boolean hasFactorioInstall() {
		return hasFactorioInstall;
	}

	public static File getFactorioInstall() {
		return factorioInstall;
	}

	public static File getFactorioExecutable() {
		return factorioExecutable;
	}
	
	public static boolean hasModPortalApi() {
		return hasModPortalApi;
	}

	public static String getModPortalApiUsername() {
		return modPortalApiUsername;
	}

	public static String getModPortalApiPassword() {
		return modPortalApiPassword;
	}

	public void initializePrototypes() throws JSONException, IOException {
		if (initializedPrototypes) {
			throw new IllegalStateException("Already Initialized Prototypes!");
		}
		initializedPrototypes = true;

		for (Profile profile : profiles) {

			FactorioData factorioData = profile.getFactorioData();

			if (!factorioData.initialize(false)) {
				LOGGER.warn("Failed to initialize Factorio data for profile: {}", profile.getName());
				System.exit(-1);
			}

			DataTable table = factorioData.getTable();

			profileByData.put(factorioData, profile);
			profile.getModLoader().getMods().keySet().forEach(mod -> profileByModName.put(mod, profile));

			recipeByName.putAll(table.getRecipes());
			itemByName.putAll(table.getItems());
			fluidByName.putAll(table.getFluids());
			entityByName.putAll(table.getEntities());
			technologyByName.putAll(table.getTechnologies());
			tileByName.putAll(table.getTiles());
			equipmentByName.putAll(table.getEquipments());
			achievementByName.putAll(table.getAchievements());
			itemGroupByName.putAll(table.getItemGroups());

			if (profile.isVanilla()) {
				profileVanilla = profile;
			}
		}

		if (profileVanilla == null) {
			throw new IllegalStateException("No vanilla profile found!");
		}

		DataTable baseTable = profileVanilla.getFactorioData().getTable();
		utilitySprites = new FPUtilitySprites(profileVanilla, baseTable.getRaw("utility-sprites", "default").get());
	}

	public void initializeFactories() throws JSONException, IOException {
		if (!initializedPrototypes) {
			throw new IllegalStateException("Must initialize prototypes first!");
		}
		if (initializedFactories) {
			throw new IllegalStateException("Already Initialized Factories!");
		}
		initializedFactories = true;

		for (Profile profile : profiles) {
			JSONObject jsonProfile = new JSONObject(
					Files.readString(profile.getFileProfile().toPath()));
			EntityRendererFactory.registerFactories(this::registerEntityFactory, profile,
					jsonProfile.getJSONObject("entities"));
			TileRendererFactory.registerFactories(this::registerTileFactory, profile,
					jsonProfile.getJSONObject("tiles"));
		}

		DataTable baseTable = profileVanilla.getFactorioData().getTable();
		
		EntityRendererFactory.initFactories(entityFactories);
		TileRendererFactory.initFactories(tileFactories);

		entityFactories.forEach(e -> profileByGroupName.put(e.getGroupName(), e.getProfile()));

		// Place vanilla protos again to be the priority
		recipeByName.putAll(baseTable.getRecipes());
		itemByName.putAll(baseTable.getItems());
		fluidByName.putAll(baseTable.getFluids());
		entityByName.putAll(baseTable.getEntities());
		technologyByName.putAll(baseTable.getTechnologies());
		tileByName.putAll(baseTable.getTiles());
		equipmentByName.putAll(baseTable.getEquipments());
		achievementByName.putAll(baseTable.getAchievements());
		itemGroupByName.putAll(baseTable.getItemGroups());

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

	public Optional<AchievementPrototype> lookupAchievementByName(String name) {
		return Optional.ofNullable(achievementByName.get(name));
	}

	public Profile lookupProfileByGroupName(String groupName) {
		return profileByGroupName.get(groupName);
	}

	public Profile lookupProfileByData(FactorioData data) {
		return profileByData.get(data);
	}

	public Optional<EntityPrototype> lookupEntityByName(String name) {
		return Optional.ofNullable(entityByName.get(name));
	}

	public EntityRendererFactory lookupEntityFactoryForName(String name) {
		return Optional.ofNullable(entityFactoryByName.get(name)).orElseGet(() -> {
			try {
				return unknownEntityFactories.get(name, () -> new UnknownEntityRendering(profileVanilla, name));
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
		});
	}

	public Optional<EquipmentPrototype> lookupEquipmentByName(String name) {
		return Optional.ofNullable(equipmentByName.get(name));
	}

	public Optional<FluidPrototype> lookupFluidByName(String name) {
		return Optional.ofNullable(fluidByName.get(name));
	}

	public Optional<ItemPrototype> lookupItemByName(String name) {
		return Optional.ofNullable(itemByName.get(name));
	}

	public Optional<ItemGroupPrototype> lookupItemGroupByName(String name) {
		return Optional.ofNullable(itemGroupByName.get(name));
	}

	public BufferedImage lookupModImage(String filename) {
		if (!hasFactorioInstall) {
			LOGGER.error("FACTORIO INSTALL NEEDED TO LOAD IMAGES!");
			System.exit(-1);
			return null;
		}

		try {
			String firstSegment = filename.split("\\/")[0];
			String modName = firstSegment.substring(2, firstSegment.length() - 2);
			return profileByModName.get(modName).get(0).getModLoader().getModImage(filename);
		} catch (Exception e) {
			LOGGER.error("FILENAME: {}", filename);
			throw e;
		}
	}

	public Optional<RecipePrototype> lookupRecipeByName(String name) {
		return Optional.ofNullable(recipeByName.get(name));
	}

	public Optional<TechPrototype> lookupTechnologyByName(String name) {
		return Optional.ofNullable(technologyByName.get(name));
	}

	public Optional<TilePrototype> lookupTileByName(String name) {
		return Optional.ofNullable(tileByName.get(name));
	}

	public TileRendererFactory lookupTileFactoryForName(String name) {
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

	private synchronized void registerEntityFactory(EntityRendererFactory factory) {
		String name = factory.getPrototype().getName();

		if (entityFactoryByName.containsKey(name)) {
			EntityRendererFactory existingFactory = entityFactoryByName.get(name);

			String detailMessage = String.format(
					"Entity '%s' is already registered in group '%s' from profile '%s'. Attempted re-registration from profile '%s' is not allowed.",
					name, existingFactory.getGroupName(), existingFactory.getProfile().getName(),
					factory.getProfile().getName());
			throw new IllegalArgumentException(detailMessage);
		}

		entityFactories.add(factory);
		entityFactoryByName.put(name, factory);
	}

	private synchronized void registerTileFactory(TileRendererFactory factory) {
		String name = factory.getPrototype().getName();
		if (tileFactoryByName.containsKey(name)) {
			throw new IllegalArgumentException("Tile already exists! " + name);
		}
		tileFactories.add(factory);
		tileFactoryByName.put(name, factory);
	}

    public static String getFactorioVersion() {
		return factorioVersion;
    }
}
