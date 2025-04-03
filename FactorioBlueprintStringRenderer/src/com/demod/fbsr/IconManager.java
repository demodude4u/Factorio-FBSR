package com.demod.fbsr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemSubGroupPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.RichText.TagToken;
import com.demod.fbsr.def.IconDef;
import com.google.common.collect.ImmutableMap;

public class IconManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(IconManager.class);

	public static final int ICON_SIZE = 64;

//	private static final Map<String, IconCategory> tagCategories = new HashMap<>();
//	static {
//		tagCategories.put("item", IconCategory.ITEM);
//		tagCategories.put("entity", IconCategory.ENTITY);
//		tagCategories.put("technology", IconCategory.TECHNOLOGY);
//		tagCategories.put("recipe", IconCategory.RECIPE);
//		tagCategories.put("item-group", IconCategory.ITEM_GROUP);
//		tagCategories.put("fluid", IconCategory.FLUID);
//		tagCategories.put("tile", IconCategory.TILE);
//		tagCategories.put("virtual-signal", IconCategory.VIRTUAL_SIGNAL);
//		tagCategories.put("achievement", IconCategory.ACHIEVEMENT);
//		tagCategories.put("armor", IconCategory.ARMOR);
//		tagCategories.put("shortcut", IconCategory.SHORTCUT);
//		tagCategories.put("quality", IconCategory.QUALITY);
//		tagCategories.put("planet", IconCategory.PLANET);
//		tagCategories.put("space-location", IconCategory.SPACE_LOCATION);
//	}
//	private static final Map<String, Supplier<ImageDef>> tagPlaceholders = new HashMap<>();
//	static {
//
//	}
	// Details found at https://wiki.factorio.com/rich_text
	// Tags that need placeholders or fancy parsing:
	// TODO img
	// TODO gps
	// TODO special-item
	// TODO train
	// TODO train-stop
	// TODO tip
	// TODO tooltip
	// TODO space-platform
	// TODO space-age

	public static class PrototypeResolver extends IconResolver {
		protected final Map<String, ? extends DataPrototype> map;
		protected final Map<String, IconDef> defs;

		public PrototypeResolver(String category, Map<String, ? extends DataPrototype> map) {
			super(category);

			this.map = map;

			defs = createDefs();

			defs.values().forEach(AtlasManager::registerDef);
		}

		protected Map<String, IconDef> createDefs() {
			Map<String, IconDef> defs = new HashMap<>();
			for (Entry<String, ? extends DataPrototype> entry : map.entrySet()) {
				DataPrototype proto = entry.getValue();
				List<IconLayer> layers = IconLayer.fromPrototype(proto.lua());
				defs.put(entry.getKey(), new IconDef("TAG[" + category + "]/" + entry.getKey() + "/" + ICON_SIZE,
						layers, ICON_SIZE, proto));
			}
			return defs;
		}

		@Override
		public Optional<IconDef> lookup(String key) {
			return Optional.ofNullable(defs.get(key));
		}
	}

	public static class RecipeResolver extends PrototypeResolver {

		public RecipeResolver(String category, Map<String, RecipePrototype> map) {
			super(category, map);
		}

		@Override
		protected Map<String, IconDef> createDefs() {
			Map<String, IconDef> defs = new HashMap<>();
			for (Entry<String, ? extends DataPrototype> entry : map.entrySet()) {
				DataPrototype proto = entry.getValue();

				// TODO confirm if the recipe or the product is used in sorting!

				LuaTable lua = proto.lua();
				List<IconLayer> layers;
				DataPrototype iconProto;
				if (!lua.get("icon").isnil() || !lua.get("icons").isnil()) {
					layers = IconLayer.fromPrototype(lua);
					iconProto = proto;

				} else {
					String resultName = null;
					if (!lua.get("main_product").isnil()) {
						resultName = lua.get("main_product").tojstring();

					} else if (lua.get("results").length() == 1) {
						LuaTable luaResult = lua.get("results").totableArray().get(1).totableObject();
						String resultType = luaResult.get("type").tojstring();

						if (resultType.equals("item")) {
							resultName = luaResult.get("name").tojstring();

						} else if (resultType.equals("research-progress")) {
							resultName = luaResult.get("research_item").tojstring();

						} else if (resultType.equals("fluid")) {
							resultName = luaResult.get("name").tojstring();
						}

					}

					Optional<? extends DataPrototype> luaProduct = FactorioManager.lookupItemByName(resultName);
					if (luaProduct.isEmpty()) {
						luaProduct = FactorioManager.lookupFluidByName(resultName);
						if (luaProduct.isEmpty()) {
							LOGGER.warn("Unable to find recipe result! {} ({})", entry.getKey(), resultName);
							continue;
						}
					}

					layers = IconLayer.fromPrototype(luaProduct.get().lua());
					iconProto = luaProduct.get();
				}

				defs.put(entry.getKey(), new IconDef("TAG[" + category + "]/" + entry.getKey() + "/" + ICON_SIZE,
						layers, ICON_SIZE, iconProto));
			}
			return defs;
		}
	}

	public static abstract class IconResolver {
		protected final String category;

		public IconResolver(String category) {
			this.category = category;
		}

		public String getCategory() {
			return category;
		}

		public abstract Optional<IconDef> lookup(String key);

		public static IconResolver forRecipes(String category, List<RecipePrototype> prototypes) {
			Map<String, RecipePrototype> map = new LinkedHashMap<>();
			for (RecipePrototype proto : prototypes) {
				map.put(proto.getName(), proto);
			}
			return new RecipeResolver(category, map);
		}

		public static IconResolver forPrototypes(String category, List<? extends DataPrototype> prototypes) {
			Map<String, DataPrototype> map = new LinkedHashMap<>();
			for (DataPrototype proto : prototypes) {
				map.put(proto.getName(), proto);
			}
			return new PrototypeResolver(category, map);
		}

		public static IconResolver forPath(String... rawPaths) {
			Map<String, DataPrototype> map = new LinkedHashMap<>();
			for (String rawPath : rawPaths) {
				String[] path = rawPath.split("\\.");
				for (FactorioData data : FactorioManager.getDatas()) {
					LuaTable lua = data.getTable().getRaw(path).get().totableObject();
					Utils.forEach(lua, (k, v) -> {
						DataPrototype proto = new DataPrototype(v.totableObject());
						Optional<ItemSubGroupPrototype> subgroup = proto.getSubgroup()
								.flatMap(s -> data.getTable().getItemSubgroup(s));
						if (subgroup.isPresent()) {
							proto.setGroup(subgroup.get().getGroup());
						} else {
							proto.setGroup(Optional.empty());
						}
						map.put(k.tojstring(), proto);
					});
				}
			}
			return new PrototypeResolver("path-" + Arrays.stream(rawPaths).collect(Collectors.joining("-")), map);
		}
	}

	private static final Map<String, Function<String, Optional<IconDef>>> signalResolvers = ImmutableMap
			.<String, Function<String, Optional<IconDef>>>builder()//
			.put("item", IconManager::lookupItem)//
			.put("fluid", IconManager::lookupFluid)//
			.put("virtual", IconManager::lookupVirtualSignal)//
			.put("entity", IconManager::lookupEntity)//
			.put("recipe", IconManager::lookupRecipe)//
			.put("space-location", IconManager::lookupSpaceLocation)//
			.put("asteroid-chunk", IconManager::lookupAsteroidChunk)//
			.put("quality", IconManager::lookupQuality)//
			.build();

	private static volatile boolean initialized = false;

	private static IconResolver itemResolver;
	private static IconResolver entityResolver;
	private static IconResolver technologyResolver;
	private static IconResolver recipeResolver;
	private static IconResolver itemGroupResolver;
	private static IconResolver fluidResolver;
	private static IconResolver tileResolver;
	private static IconResolver virtualSignalResolver;
	private static IconResolver achievementResolver;
	private static IconResolver armorResolver;
	private static IconResolver shortcutResolver;
	private static IconResolver qualityResolver;
	private static IconResolver planetResolver;
	private static IconResolver spaceLocationResolver;
	private static IconResolver asteroidChunkResolver;

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		itemResolver = IconResolver.forPrototypes("item", FactorioManager.getItems());
		entityResolver = IconResolver.forPrototypes("entity", FactorioManager.getEntities());
		technologyResolver = IconResolver.forPrototypes("technology", FactorioManager.getTechnologies());
		recipeResolver = IconResolver.forRecipes("recipe", FactorioManager.getRecipes());
		itemGroupResolver = IconResolver.forPrototypes("item-group", FactorioManager.getItemGroups());
		fluidResolver = IconResolver.forPrototypes("fluid", FactorioManager.getFluids());
		tileResolver = IconResolver.forPrototypes("tile", FactorioManager.getTiles());
		virtualSignalResolver = IconResolver.forPath("virtual-signal");
		achievementResolver = IconResolver.forPrototypes("achievement", FactorioManager.getAchievements());
		armorResolver = IconResolver.forPath("armor");
		shortcutResolver = IconResolver.forPath("shortcut");
		qualityResolver = IconResolver.forPath("quality");
		planetResolver = IconResolver.forPath("planet");
		spaceLocationResolver = IconResolver.forPath("space-location", "planet");
		asteroidChunkResolver = IconResolver.forPath("asteroid-chunk");
	}

	public static Optional<IconDef> lookupItem(String name) {
		return itemResolver.lookup(name);
	}

	public static Optional<IconDef> lookupEntity(String name) {
		return entityResolver.lookup(name);
	}

	public static Optional<IconDef> lookupTechnology(String name) {
		return technologyResolver.lookup(name);
	}

	public static Optional<IconDef> lookupRecipe(String name) {
		return recipeResolver.lookup(name);
	}

	public static Optional<IconDef> lookupItemGroup(String name) {
		return itemGroupResolver.lookup(name);
	}

	public static Optional<IconDef> lookupFluid(String name) {
		return fluidResolver.lookup(name);
	}

	public static Optional<IconDef> lookupTile(String name) {
		return tileResolver.lookup(name);
	}

	public static Optional<IconDef> lookupVirtualSignal(String name) {
		return virtualSignalResolver.lookup(name);
	}

	public static Optional<IconDef> lookupachievement(String name) {
		return achievementResolver.lookup(name);
	}

	public static Optional<IconDef> lookupArmor(String name) {
		return armorResolver.lookup(name);
	}

	public static Optional<IconDef> lookupShortcut(String name) {
		return shortcutResolver.lookup(name);
	}

	public static Optional<IconDef> lookupQuality(String name) {
		return qualityResolver.lookup(name);
	}

	public static Optional<IconDef> lookupPlanet(String name) {
		return planetResolver.lookup(name);
	}

	public static Optional<IconDef> lookupSpaceLocation(String name) {
		return spaceLocationResolver.lookup(name);
	}

	public static Optional<IconDef> lookupAsteroidChunk(String name) {
		return asteroidChunkResolver.lookup(name);
	}

	public static Optional<IconDef> lookupSignalID(String type, String name) {
		Function<String, Optional<IconDef>> lookup = signalResolvers.get(type);
		if (lookup != null) {
			return lookup.apply(name);
		} else {
			return Optional.empty();
		}
	}

	public static Optional<IconDefWithQuality> lookupTag(TagToken tag) {
		// TODO
		return Optional.empty();
	}

	public static Optional<IconDefWithQuality> lookupFilter(Optional<String> type, Optional<String> name,
			Optional<String> quality) {
		type = type.or(() -> Optional.of("item"));

		if (name.isEmpty() && quality.isEmpty()) {
			return Optional.empty();
		}

		if (name.isPresent()) {
			return lookupSignalID(type.get(), name.get()).map(def -> new IconDefWithQuality(def, quality));

		} else {
			return lookupQuality(quality.get()).map(def -> new IconDefWithQuality(def, Optional.empty()));
		}
	}
}
