package com.demod.fbsr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModLoader;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.EquipmentPrototype;
import com.demod.factorio.prototype.FluidPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TechPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.bs.BSEntity;

public class FactorioManager {

	private static volatile boolean initializedPrototypes = false;
	private static volatile boolean initializedFactories = false;

	private static final List<FactorioData> datas = new ArrayList<>();

	private static final Map<String, FactorioData> dataByModName = new HashMap<>();

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

	public static void initializeFactories() {
		if (initializedFactories) {
			throw new IllegalStateException("Already Initialized Factories!");
		}
		initializedFactories = true;

		EntityRendererFactory.initFactories(entityFactories);
		TileRendererFactory.initFactories(tileFactories);
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

		for (File folderMods : folderModsRoot.listFiles()) {
			if (!folderMods.isDirectory()) {
				continue;
			}
			if (!new File(folderMods, "mod-list.json").exists()) {
				continue;
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

			JSONObject jsonModRendering = new JSONObject(
					Files.readString(new File(folderMods, "mod-rendering.json").toPath()));
			EntityRendererFactory.registerFactories(FactorioManager::registerEntityFactory, data,
					jsonModRendering.getJSONObject("entities"));
			TileRendererFactory.registerFactories(FactorioManager::registerTileFactory, data,
					jsonModRendering.getJSONObject("tiles"));

			recipeByName.putAll(data.getTable().getRecipes());
			itemByName.putAll(data.getTable().getItems());
			fluidByName.putAll(data.getTable().getFluids());
			entityByName.putAll(data.getTable().getEntities());
			technologyByName.putAll(data.getTable().getTechnologies());
			tileByName.putAll(data.getTable().getTiles());
			equipmentByName.putAll(data.getTable().getEquipments());
		}
	}

	public static FactorioData lookupDataForModName(String modName) {
		return dataByModName.get(modName);
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
	private static synchronized void registerEntityFactory(String name, EntityRendererFactory factory) {
		if (entityFactoryByName.containsKey(name)) {
			throw new IllegalArgumentException("Entity already exists! " + name);
		}
		entityFactories.add(factory);
		entityFactoryByName.put(name, factory);
	}

	private static synchronized void registerTileFactory(String name, TileRendererFactory factory) {
		if (tileFactoryByName.containsKey(name)) {
			throw new IllegalArgumentException("Tile already exists! " + name);
		}
		tileFactories.add(factory);
		tileFactoryByName.put(name, factory);
	}
}
