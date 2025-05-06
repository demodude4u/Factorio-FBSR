package com.demod.fbsr;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.FocusManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemSubGroupPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.RichText.TagToken;
import com.demod.fbsr.composite.TintComposite;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPUtilitySprites;
import com.google.common.collect.ImmutableMap;

public class IconManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(IconManager.class);

	public static final int ICON_SIZE = 64;

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
						proto.setTable(data.getTable());
						map.put(k.tojstring(), proto);
					});
				}
			}
			return new PrototypeResolver("path-" + Arrays.stream(rawPaths).collect(Collectors.joining("-")), map);
		}
	}

	public static abstract class TagResolver {
		public abstract Optional<TagWithQuality> lookup(TagToken tag);
	}

	public static class DelegateTagResolver extends TagResolver {
		private final IconResolver resolver;

		public DelegateTagResolver(IconResolver resolver) {
			this.resolver = resolver;
		}

		@Override
		public Optional<TagWithQuality> lookup(TagToken tag) {
			return resolver.lookup(tag.value).map(def -> new TagWithQuality(def, tag.quality));
		}
	}

	public static class PlaceholderTagResolver extends TagResolver {
		private final ImageDef def;

		public PlaceholderTagResolver(FPSprite sprite){
			def = new ImageDef("[TAG]" + sprite.filename.get() + "/" + ICON_SIZE, k -> convertSprite(sprite), new Rectangle(ICON_SIZE, ICON_SIZE));
			
			AtlasManager.registerDef(def);
		}

		public PlaceholderTagResolver(IconResolver resolver, String name) {
			def = resolver.lookup(name).get();
		}

		private BufferedImage convertSprite(FPSprite sprite) {
			BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = icon.createGraphics();
			Composite pc = g.getComposite();

			List<SpriteDef> defs = sprite.defineSprites();
			if (defs.size() != 1) {
				throw new IllegalArgumentException("Placeholder tag resolver only supports single layer sprites!");
			}
			SpriteDef def = defs.get(0);

			BufferedImage imageSheet = FactorioManager.lookupModImage(def.getPath());

			if (def.getTint().isPresent() && !def.getTint().get().equals(Color.white)) {
				g.setComposite(new TintComposite(def.getTint().get()));
			} else {
				g.setComposite(pc);
			}

			Rectangle source = def.getSource();
			g.drawImage(imageSheet, 0, 0, ICON_SIZE, ICON_SIZE,
					source.x, source.y, source.x + source.width, source.y + source.height, null);
			g.dispose();

			return icon;
		}

		@Override
		public Optional<TagWithQuality> lookup(TagToken tag) {
			return Optional.of(new TagWithQuality(def, tag.quality));
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

	private static Map<String, TagResolver> tagResolvers = new HashMap<>();

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

		FPUtilitySprites utilitySprites = FactorioManager.getUtilitySprites();
		//TODO img should at least resolve class.name format
		tagResolvers.put("img", new PlaceholderTagResolver(utilitySprites.questionmark));
		tagResolvers.put("item", new DelegateTagResolver(itemResolver));
		tagResolvers.put("entity", new DelegateTagResolver(entityResolver));
		tagResolvers.put("technology", new DelegateTagResolver(technologyResolver));
		tagResolvers.put("recipe", new DelegateTagResolver(recipeResolver));
		tagResolvers.put("item-group", new DelegateTagResolver(itemGroupResolver));
		tagResolvers.put("fluid", new DelegateTagResolver(fluidResolver));
		tagResolvers.put("tile", new DelegateTagResolver(tileResolver));
		tagResolvers.put("virtual-signal", new DelegateTagResolver(virtualSignalResolver));
		tagResolvers.put("virtual", new DelegateTagResolver(virtualSignalResolver));
		tagResolvers.put("achievement", new DelegateTagResolver(achievementResolver));
		tagResolvers.put("gps", new PlaceholderTagResolver(utilitySprites.gpsMapIcon));
		tagResolvers.put("special-item", new PlaceholderTagResolver(itemResolver, "blueprint"));
		tagResolvers.put("armor", new DelegateTagResolver(armorResolver));
		tagResolvers.put("train", new PlaceholderTagResolver(itemResolver, "locomotive"));
		tagResolvers.put("train-stop", new PlaceholderTagResolver(itemResolver, "train-stop"));
		tagResolvers.put("shortcut", new DelegateTagResolver(shortcutResolver));
		tagResolvers.put("tip", new PlaceholderTagResolver(utilitySprites.tipIcon));
		tagResolvers.put("tooltip", new PlaceholderTagResolver(utilitySprites.customTagIcon));
		tagResolvers.put("quality", new DelegateTagResolver(qualityResolver));
		tagResolvers.put("space-platform", new PlaceholderTagResolver(itemResolver, "space-platform-hub"));
		tagResolvers.put("planet", new DelegateTagResolver(planetResolver));
		tagResolvers.put("space-location", new DelegateTagResolver(spaceLocationResolver));
		tagResolvers.put("space-age", new PlaceholderTagResolver(utilitySprites.spaceAgeIcon));
		tagResolvers.put("asteroid-chunk", new DelegateTagResolver(asteroidChunkResolver));
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

	public static Optional<IconDefWithQuality> lookupSignalID(String type, String name, Optional<String> quality) {
		Function<String, Optional<IconDef>> lookup = signalResolvers.get(type);
		if (lookup != null) {
			return lookup.apply(name).map(def -> new IconDefWithQuality(def, quality));
		} else {
			return Optional.empty();
		}
	}

	public static Optional<TagWithQuality> lookupTag(TagToken tag) {
		return Optional.ofNullable(tagResolvers.get(tag.name)).flatMap(r-> r.lookup(tag));
	}

	public static Optional<IconDefWithQuality> lookupFilter(Optional<String> type, Optional<String> name,
			Optional<String> quality) {
		type = type.or(() -> Optional.of("item"));

		if (name.isEmpty() && quality.isEmpty()) {
			return Optional.empty();
		}

		if (name.isPresent()) {
			return lookupSignalID(type.get(), name.get(), quality);

		} else {
			return lookupQuality(quality.get()).map(def -> new IconDefWithQuality(def, Optional.empty()));
		}
	}
}
