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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class FactorioManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(FactorioManager.class);

	private static boolean hasFactorioInstall;
	private static File factorioInstall;
	private static Optional<File> factorioExecutableOverride;
	private static String factorioVersion;

	private static boolean hasModPortalApi;
	private static String modPortalApiUsername;
	private static String modPortalApiPassword;


	static {
		reloadConfig();
	}

	public static void reloadConfig() {
		Config config = Config.load();
		
		if (config.factorio.install != null) {
			hasFactorioInstall = true;
			factorioInstall = new File(config.factorio.install);
			factorioExecutableOverride = Optional.ofNullable(config.factorio.executable).map(path -> new File(factorioInstall, path));
			factorioVersion = FactorioData.getVersionFromInstall(factorioInstall, factorioExecutableOverride).get();
		} else {
			hasFactorioInstall = false;
			factorioInstall = null;
			factorioExecutableOverride = null;
			factorioVersion = null;
		}

		if (config.modportal.username != null && config.modportal.password != null) {
			hasModPortalApi = true;
			modPortalApiUsername = config.modportal.username;
			modPortalApiPassword = config.modportal.password;
		} else {
			hasModPortalApi = false;
			modPortalApiUsername = null;
			modPortalApiPassword = null;
		}
	}

	private volatile boolean initializedPrototypes = false;
	private volatile boolean initializedFactories = false;

	private final List<Profile> profiles;
	
	private IconManager iconManager;
	private Profile profileVanilla = null;

	private final Map<FactorioData, Profile> profileByData = new HashMap<>();
	private final ListMultimap<String, Profile> profileByModName = ArrayListMultimap.create();
	private final ListMultimap<String, Profile> profileByEntityName = ArrayListMultimap.create();
	private final ListMultimap<String, Profile> profileByTileName = ArrayListMultimap.create();

	private final ListMultimap<String, EntityRendererFactory> entityFactoryByName = ArrayListMultimap.create();
	private final ListMultimap<String, TileRendererFactory> tileFactoryByName = ArrayListMultimap.create();

	private final ListMultimap<String, ItemPrototype> itemByName = ArrayListMultimap.create();
	private final ListMultimap<String, RecipePrototype> recipeByName = ArrayListMultimap.create();
	private final ListMultimap<String, FluidPrototype> fluidByName = ArrayListMultimap.create();
	private final ListMultimap<String, TechPrototype> technologyByName = ArrayListMultimap.create();
	private final ListMultimap<String, EntityPrototype> entityByName = ArrayListMultimap.create();
	private final ListMultimap<String, TilePrototype> tileByName = ArrayListMultimap.create();
	private final ListMultimap<String, EquipmentPrototype> equipmentByName = ArrayListMultimap.create();
	private final ListMultimap<String, AchievementPrototype> achievementByName = ArrayListMultimap.create();
	private final ListMultimap<String, ItemGroupPrototype> itemGroupByName = ArrayListMultimap.create();

	private FPUtilitySprites utilitySprites;

	private final Cache<String, UnknownEntityRendering> unknownEntityFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();
	private final Cache<String, UnknownTileRendering> unknownTileFactories = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	public FactorioManager(List<Profile> profiles) {
		this.profiles = profiles;
	}

	public ListMultimap<String, AchievementPrototype> getAchievementByNameMap() {
		return achievementByName;
	}

	public Profile getProfileVanilla() {
		return profileVanilla;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public IconManager getIconManager() {
		return iconManager;
	}

	public void setIconManager(IconManager iconManager) {
		this.iconManager = iconManager;
	}

	public ListMultimap<String, EntityPrototype> getEntityByNameMap() {
		return entityByName;
	}

	public ListMultimap<String, EntityRendererFactory> getEntityFactoryByNameMap() {
		return entityFactoryByName;
	}

	public ListMultimap<String, EquipmentPrototype> getEquipmentByNameMap() {
		return equipmentByName;
	}

	public ListMultimap<String, FluidPrototype> getFluidByNameMap() {
		return fluidByName;
	}

	public ListMultimap<String, ItemPrototype> getItemByNameMap() {
		return itemByName;
	}

	public ListMultimap<String, ItemGroupPrototype> getItemGroupByNameMap() {
		return itemGroupByName;
	}

	public ListMultimap<String, RecipePrototype> getRecipeByNameMap() {
		return recipeByName;
	}

	public ListMultimap<String, TechPrototype> getTechnologyByNameMap() {
		return technologyByName;
	}

	public ListMultimap<String, TileRendererFactory> getTileFactoryByNameMap() {
		return tileFactoryByName;
	}

	public ListMultimap<String, TilePrototype> getTileByNameMap() {
		return tileByName;
	}

	public FPUtilitySprites getUtilitySprites() {
		return utilitySprites;
	}

	public ListMultimap<String, Profile> getProfileByModNameMap() {
		return profileByModName;
	}

	public static boolean hasFactorioInstall() {
		return hasFactorioInstall;
	}

	public static File getFactorioInstall() {
		return factorioInstall;
	}

	public static Optional<File> getFactorioExecutableOverride() {
		return factorioExecutableOverride;
	}

	public static File getFactorioExecutable() {
		return factorioExecutableOverride.orElseGet(() -> FactorioData.getFactorioExecutable(factorioInstall));
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

	public boolean initializePrototypes() {
		if (initializedPrototypes) {
			throw new IllegalStateException("Already Initialized Prototypes!");
		}
		initializedPrototypes = true;

		for (Profile profile : profiles) {

			FactorioData factorioData = profile.getFactorioData();

			if (!factorioData.initialize(false)) {
				System.out.println("Failed to initialize Factorio data for profile: " + profile.getName());
				return false;
			}

			DataTable table = factorioData.getTable();

			profileByData.put(factorioData, profile);
			profile.listMods().forEach(mod -> profileByModName.put(mod.name, profile));
			if (profile.isVanilla()) {
				profileByModName.put("base", profile);
				profileByModName.put("core", profile);
			}

			table.getRecipes().forEach(recipeByName::put);
			table.getItems().forEach(itemByName::put);
			table.getFluids().forEach(fluidByName::put);
			table.getEntities().forEach(entityByName::put);
			table.getTechnologies().forEach(technologyByName::put);
			table.getTiles().forEach(tileByName::put);
			table.getEquipments().forEach(equipmentByName::put);
			table.getAchievements().forEach(achievementByName::put);
			table.getItemGroups().forEach(itemGroupByName::put);

			if (profile.isVanilla()) {
				profileVanilla = profile;
			}
		}

		if (profileVanilla == null) {
			System.out.println("No vanilla profile found!");
			return false;
		}

		DataTable baseTable = profileVanilla.getFactorioData().getTable();
		utilitySprites = new FPUtilitySprites(profileVanilla, baseTable.getRaw("utility-sprites", "default").get());
	
		return true;
	}

	public boolean initializeFactories() {
		if (!initializedPrototypes) {
			throw new IllegalStateException("Must initialize prototypes first!");
		}
		if (initializedFactories) {
			return true;
		}
		initializedFactories = true;

		Optional<Profile> optVanillaProfile = profiles.stream().filter(p -> p.isVanilla()).findAny();
		if (optVanillaProfile.isEmpty()) {
			LOGGER.error("Vanilla profile is missing!");
			return false;
		}
		Profile profileVanilla = optVanillaProfile.get();

		for (Profile profile : profiles) {
			RenderingRegistry registry = profile.getRenderingRegistry();

			for (EntityRendererFactory factory : registry.getEntityFactories()) {
				entityFactoryByName.put(factory.getName(), factory);
				profileByEntityName.put(factory.getName(), profile);
			}

			for (TileRendererFactory factory : registry.getTileFactories()) {
				tileFactoryByName.put(factory.getName(), factory);
				profileByTileName.put(factory.getName(), profile);
			}
		}

		for (Profile profile : profiles) {
			RenderingRegistry registry = profile.getRenderingRegistry();

			if (!registry.initializeFactories()) {
				return false;
			}
		}

		return true;
	}

	public List<AchievementPrototype> lookupAchievementByName(String name) {
		return achievementByName.get(name);
	}

	public List<Profile> lookupProfileByEntityName(String entityName) {
		return profileByEntityName.get(entityName);
	}

	public List<Profile> lookupProfileByTileName(String tileName) {
		return profileByTileName.get(tileName);
	}

	public Profile lookupProfileByData(FactorioData data) {
		return profileByData.get(data);
	}

	public List<Profile> lookupProfileByModName(String modName) {
		return profileByModName.get(modName);
	}

	public List<EntityPrototype> lookupEntityByName(String name) {
		return entityByName.get(name);
	}

	public List<EntityRendererFactory> lookupEntityFactoryForName(String name) {
		return entityFactoryByName.get(name);
	}

	public UnknownEntityRendering getUnknownEntityRenderingForName(String name) {
		try {
			return unknownEntityFactories.get(name, () -> new UnknownEntityRendering(profileVanilla, name));
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	public List<EquipmentPrototype> lookupEquipmentByName(String name) {
		return equipmentByName.get(name);
	}

	public List<FluidPrototype> lookupFluidByName(String name) {
		return fluidByName.get(name);
	}

	public List<ItemPrototype> lookupItemByName(String name) {
		return itemByName.get(name);
	}

	public List<ItemGroupPrototype> lookupItemGroupByName(String name) {
		return itemGroupByName.get(name);
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

	public List<RecipePrototype> lookupRecipeByName(String name) {
		return recipeByName.get(name);
	}

	public List<TechPrototype> lookupTechnologyByName(String name) {
		return technologyByName.get(name);
	}

	public List<TilePrototype> lookupTileByName(String name) {
		return tileByName.get(name);
	}

	public List<TileRendererFactory> lookupTileFactoryForName(String name) {
		return tileFactoryByName.get(name);
	}

	public UnknownTileRendering getUnknownTileRenderingForName(String name) {
		try {
			return unknownTileFactories.get(name, () -> new UnknownTileRendering(name));
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

    public static String getFactorioVersion() {
		return factorioVersion;
    }
}
