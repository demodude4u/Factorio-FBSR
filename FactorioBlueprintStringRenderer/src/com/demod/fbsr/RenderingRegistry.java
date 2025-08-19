package com.demod.fbsr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Profile.ManifestModInfo;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class RenderingRegistry {

	private final Profile profile;

	private final List<EntityRendererFactory> entityFactories = new ArrayList<>();
	private final List<TileRendererFactory> tileFactories = new ArrayList<>();
	private final Map<String, EntityRendererFactory> entityFactoryByName = new HashMap<>();
	private final Map<String, TileRendererFactory> tileFactoryByName = new HashMap<>();

	public RenderingRegistry(Profile profile) {
		this.profile = profile;
	}

	public List<EntityRendererFactory> getEntityFactories() {
		return entityFactories;
	}

	public List<TileRendererFactory> getTileFactories() {
		return tileFactories;
	}

	public Map<String, EntityRendererFactory> getEntityFactoryByNameMap() {
		return entityFactoryByName;
	}

	public Map<String, TileRendererFactory> getTileFactoryByNameMap() {
		return tileFactoryByName;
	}

	public Optional<EntityRendererFactory> lookupEntityFactoryByName(String name) {
		return Optional.ofNullable(entityFactoryByName.get(name));
	}

	public Optional<TileRendererFactory> lookupTileFactoryByName(String name) {
		return Optional.ofNullable(tileFactoryByName.get(name));
	}

	public void clear() {
		entityFactories.clear();
		tileFactories.clear();
		entityFactoryByName.clear();
		tileFactoryByName.clear();
	}

	@SuppressWarnings("unchecked")
	public boolean loadConfig(JSONObject jsonRendering) {
		clear();

		AtomicBoolean failed = new AtomicBoolean(false);

		Map<String, ManifestModInfo> modLookup = new HashMap<>();
		profile.listMods().forEach(mod -> modLookup.put(mod.name, mod));

		// Entities
		JSONObject jsonEntities = jsonRendering.optJSONObject("entities");
		if (jsonEntities != null) {
			for (String entityName : jsonEntities.keySet()) {
				JSONObject jsonEntity = jsonEntities.optJSONObject(entityName);
				if (jsonEntity == null) {
					continue;
				}

				String renderingClassName = jsonEntity.optString("rendering", "");
				try {
					Optional<Class<? extends EntityRendererFactory>> factoryClassOpt;
					try {
						factoryClassOpt = Optional.of((Class<? extends EntityRendererFactory>) Class.forName(renderingClassName));
					} catch (ClassNotFoundException e) {
						factoryClassOpt = Optional.empty();
					}
					if (!factoryClassOpt.isPresent()) {
						System.out.println("Entity rendering class not found: " + renderingClassName + " for " + entityName);
						failed.set(true);
						continue;
					}
					EntityRendererFactory factory = factoryClassOpt.get().getConstructor().newInstance();
					
					factory.setName(entityName);
					factory.setProfile(profile);

					List<ManifestModInfo> mods = new ArrayList<>();
					Utils.forEach(jsonEntity.getJSONArray("mods"), mod -> {
						if (!modLookup.containsKey(mod.toString())) {
							System.out.println("Mod \"" + mod.toString() + "\" not found in profile " + profile.getName());
							failed.set(true);
							return;
						}
						mods.add(modLookup.get(mod.toString()));
					});
					factory.setMods(mods);

					entityFactories.add(factory);
					entityFactoryByName.put(factory.getName(), factory);

				} catch (Exception ex) {
					System.out.println("Failed to initialize entity renderer factory for " + entityName + ": " + renderingClassName);
					ex.printStackTrace();
					failed.set(true);
				}
			}
		}

		// Tiles
		JSONObject jsonTiles = jsonRendering.optJSONObject("tiles");
		if (jsonTiles != null) {
			for (String tileName : jsonTiles.keySet()) {
				JSONObject jsonTile = jsonTiles.optJSONObject(tileName);
				if (jsonTile == null) {
					continue;
				}

				try {
					TileRendererFactory factory = new TileRendererFactory();
					
					factory.setName(tileName);
					factory.setProfile(profile);

					List<ManifestModInfo> mods = new ArrayList<>();
					Utils.forEach(jsonTile.getJSONArray("mods"), mod -> {
						if (!modLookup.containsKey(mod.toString())) {
							System.out.println("Mod \"" + mod.toString() + "\" not found in profile " + profile.getName());
							failed.set(true);
							return;
						}
						mods.add(modLookup.get(mod.toString()));
					});
					factory.setMods(mods);

					tileFactories.add(factory);
					tileFactoryByName.put(factory.getName(), factory);
				} catch (Exception ex) {
					System.out.println("Failed to initialize tile renderer factory for " + tileName);
					ex.printStackTrace();
					failed.set(true);
				}
			}
		}

		return !failed.get();
	}

	public boolean initializeFactories() {

		DataTable table = profile.getFactorioData().getTable();
		
		boolean failed = false;

		for (EntityRendererFactory factory : entityFactories) {
			String entityName = factory.getName();

			Optional<EntityPrototype> optProto = table.getEntity(entityName);
			if (!optProto.isPresent()) {
				System.out.println("Rendering entity not found in factorio data: " + entityName);
				failed = true;
				continue;
			}
			EntityPrototype proto = optProto.get();
				
			if (!factory.isEntityTypeMatch(proto)) {
				System.out.println("ENTITY MISMATCH " + entityName + " (" + factory.getClass().getSimpleName() + " ==> " + proto.getType() + ")");
				failed = true;
				continue;
			}

			factory.setPrototype(proto);
		}

		for (TileRendererFactory factory : tileFactories) {
			String tileName = factory.getName();

			Optional<TilePrototype> optProto = table.getTile(tileName);
			if (!optProto.isPresent()) {
				System.out.println("Rendering tile not found in factorio data: " + tileName);
				failed = true;
				continue;
			}
			TilePrototype proto = optProto.get();

			factory.setPrototype(proto);
		}

		for (EntityRendererFactory factory : entityFactories) {
			factory.initFromPrototype();
			factory.wirePointsById = new LinkedHashMap<>();
			factory.defineWirePoints(factory.wirePointsById::put, factory.prototype.lua());
			factory.drawBounds = factory.computeBounds();
			factory.initAtlas(profile.getAtlasPackage()::registerDef);
		}

		for (TileRendererFactory factory : tileFactories) {
			factory.initFromPrototype(profile.getFactorioData().getTable());
			factory.initAtlas(profile.getAtlasPackage()::registerDef);
		}

		if (failed) {
			System.out.println("Failed to initialize some rendering factories.");
			return false;
		}

		return true;
	}
}
