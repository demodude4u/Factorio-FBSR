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

import com.demod.factorio.DataTable;
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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;

public class IconManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(IconManager.class);

	public static final int ICON_SIZE = 64;

	public class PrototypeResolver extends IconResolver {
		protected final ListMultimap<String, ? extends DataPrototype> map;
		protected final ListMultimap<String, IconDef> defs;

		public PrototypeResolver(String category, ListMultimap<String, ? extends DataPrototype> map) {
			super(category);

			this.map = map;

			defs = createDefs();

			for (IconDef def : defs.values()) {
				def.getProfile().getAtlasPackage().registerDef(def);
			}
		}

		protected ListMultimap<String, IconDef> createDefs() {
			ListMultimap<String, IconDef> defs = ArrayListMultimap.create();
			for (Entry<String, ? extends DataPrototype> entry : map.entries()) {
				DataPrototype proto = entry.getValue();
				List<IconLayer> layers = IconLayer.fromPrototype(proto.lua());
				Profile profile = factorioManager.lookupProfileByData(proto.getTable().getData());
				defs.put(entry.getKey(), new IconDef(profile, "TAG[" + category + "]/" + entry.getKey() + "/" + ICON_SIZE,
						layers, ICON_SIZE, proto));
			}
			return defs;
		}

		@Override
		public List<IconDef> lookup(String key) {
			return defs.get(key);
		}
	}

	public class RecipeResolver extends PrototypeResolver {

		public RecipeResolver(String category, ListMultimap<String, RecipePrototype> map) {
			super(category, map);
		}

		@Override
		protected ListMultimap<String, IconDef> createDefs() {
			ListMultimap<String, IconDef> defs = ArrayListMultimap.create();
			for (Entry<String, ? extends DataPrototype> entry : map.entries()) {
				DataPrototype proto = entry.getValue();
				DataTable table = proto.getTable();

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

					Optional<? extends DataPrototype> luaProduct = table.getItem(resultName);
					if (luaProduct.isEmpty()) {
						luaProduct = table.getFluid(resultName);
						if (luaProduct.isEmpty()) {
							LOGGER.warn("Unable to find recipe result! {} ({})", entry.getKey(), resultName);
							continue;
						}
					}

					layers = IconLayer.fromPrototype(luaProduct.get().lua());
					iconProto = luaProduct.get();
				}

				Profile profile = factorioManager.lookupProfileByData(table.getData());
				defs.put(entry.getKey(), new IconDef(profile, "TAG[" + category + "]/" + entry.getKey() + "/" + ICON_SIZE,
						layers, ICON_SIZE, iconProto));
			}
			return defs;
		}
	}

	public abstract class IconResolver {
		protected final String category;

		public IconResolver(String category) {
			this.category = category;
		}

		public String getCategory() {
			return category;
		}

		public abstract List<IconDef> lookup(String key);

	}
	private IconResolver forRecipes(String category, ListMultimap<String, RecipePrototype> map) {
		return new RecipeResolver(category, map);
	}

	private IconResolver forPrototypes(String category, ListMultimap<String, ? extends DataPrototype> map) {
		return new PrototypeResolver(category, map);
	}

	private IconResolver forPath(String... rawPaths) {
		ListMultimap<String, DataPrototype> map = ArrayListMultimap.create();
		for (String rawPath : rawPaths) {
			String[] path = rawPath.split("\\.");
			for (Profile profile : factorioManager.getProfiles()) {
				FactorioData data = profile.getFactorioData();
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

	public abstract class TagResolver {
		public abstract List<TagWithQuality> lookup(TagToken tag);
	}

	public class DelegateTagResolver extends TagResolver {
		private final IconResolver resolver;

		public DelegateTagResolver(IconResolver resolver) {
			this.resolver = resolver;
		}
		
		@Override
		public List<TagWithQuality> lookup(TagToken tag) {
			return resolver.lookup(tag.value).stream().map(def -> new TagWithQuality(def, tag.quality)).collect(Collectors.toList());
		}
	}

	public class PlaceholderTagResolver extends TagResolver {
		private final List<? extends ImageDef> defs;

		public PlaceholderTagResolver(FPSprite sprite){
			ImageDef def = new ImageDef(factorioManager.getProfileVanilla(), "[TAG]" + sprite.filename.get() + "/" + ICON_SIZE, k -> convertSprite(sprite), new Rectangle(ICON_SIZE, ICON_SIZE));
			def.getProfile().getAtlasPackage().registerDef(def);
			defs = ImmutableList.of(def);
		}

		public PlaceholderTagResolver(IconResolver resolver, String name) {
			defs = resolver.lookup(name);
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

			BufferedImage imageSheet = factorioManager.lookupModImage(def.getPath());

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
		public List<TagWithQuality> lookup(TagToken tag) {
			return defs.stream()
					.map(def -> new TagWithQuality(def, tag.quality))
					.collect(Collectors.toList());
		}
	}

	private final Map<String, Function<String, List<IconDef>>> signalResolvers = ImmutableMap
			.<String, Function<String, List<IconDef>>>builder()//
			.put("item", this::lookupItem)//
			.put("fluid", this::lookupFluid)//
			.put("virtual", this::lookupVirtualSignal)//
			.put("entity", this::lookupEntity)//
			.put("recipe", this::lookupRecipe)//
			.put("space-location", this::lookupSpaceLocation)//
			.put("asteroid-chunk", this::lookupAsteroidChunk)//
			.put("quality", this::lookupQuality)//
			.build();

	private final FactorioManager factorioManager;

	private volatile boolean initialized = false;

	private IconResolver itemResolver;
	private IconResolver entityResolver;
	private IconResolver technologyResolver;
	private IconResolver recipeResolver;
	private IconResolver itemGroupResolver;
	private IconResolver fluidResolver;
	private IconResolver tileResolver;
	private IconResolver virtualSignalResolver;
	private IconResolver achievementResolver;
	private IconResolver armorResolver;
	private IconResolver shortcutResolver;
	private IconResolver qualityResolver;
	private IconResolver planetResolver;
	private IconResolver spaceLocationResolver;
	private IconResolver asteroidChunkResolver;

	private Map<String, TagResolver> tagResolvers = new HashMap<>();

	public IconManager(FactorioManager factorioManager) {
		this.factorioManager = factorioManager;
		factorioManager.setIconManager(this);
	}

	public void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		itemResolver = forPrototypes("item", factorioManager.getItemByNameMap());
		entityResolver = forPrototypes("entity", factorioManager.getEntityByNameMap());
		technologyResolver = forPrototypes("technology", factorioManager.getTechnologyByNameMap());
		recipeResolver = forRecipes("recipe", factorioManager.getRecipeByNameMap());
		itemGroupResolver = forPrototypes("item-group", factorioManager.getItemGroupByNameMap());
		fluidResolver = forPrototypes("fluid", factorioManager.getFluidByNameMap());
		tileResolver = forPrototypes("tile", factorioManager.getTileByNameMap());
		virtualSignalResolver = forPath("virtual-signal");
		achievementResolver = forPrototypes("achievement", factorioManager.getAchievementByNameMap());
		armorResolver = forPath("armor");
		shortcutResolver = forPath("shortcut");
		qualityResolver = forPath("quality");
		planetResolver = forPath("planet");
		spaceLocationResolver = forPath("space-location", "planet");
		asteroidChunkResolver = forPath("asteroid-chunk");

		FPUtilitySprites utilitySprites = factorioManager.getUtilitySprites();
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

	public List<IconDef> lookupItem(String name) {
		return itemResolver.lookup(name);
	}

	public List<IconDef> lookupEntity(String name) {
		return entityResolver.lookup(name);
	}

	public List<IconDef> lookupTechnology(String name) {
		return technologyResolver.lookup(name);
	}

	public List<IconDef> lookupRecipe(String name) {
		return recipeResolver.lookup(name);
	}

	public List<IconDef> lookupItemGroup(String name) {
		return itemGroupResolver.lookup(name);
	}

	public List<IconDef> lookupFluid(String name) {
		return fluidResolver.lookup(name);
	}

	public List<IconDef> lookupTile(String name) {
		return tileResolver.lookup(name);
	}

	public List<IconDef> lookupVirtualSignal(String name) {
		return virtualSignalResolver.lookup(name);
	}

	public List<IconDef> lookupAchievement(String name) {
		return achievementResolver.lookup(name);
	}

	public List<IconDef> lookupArmor(String name) {
		return armorResolver.lookup(name);
	}

	public List<IconDef> lookupShortcut(String name) {
		return shortcutResolver.lookup(name);
	}

	public List<IconDef> lookupQuality(String name) {
		return qualityResolver.lookup(name);
	}

	public List<IconDef> lookupPlanet(String name) {
		return planetResolver.lookup(name);
	}

	public List<IconDef> lookupSpaceLocation(String name) {
		return spaceLocationResolver.lookup(name);
	}

	public List<IconDef> lookupAsteroidChunk(String name) {
		return asteroidChunkResolver.lookup(name);
	}

	public List<IconDefWithQuality> lookupSignalID(String type, String name, Optional<String> quality) {
		Function<String, List<IconDef>> lookup = signalResolvers.get(type);
		if (lookup != null) {
			return lookup.apply(name).stream()
					.map(def -> new IconDefWithQuality(def, quality))
					.collect(Collectors.toList());
		} else {
			return ImmutableList.of();
		}
	}

	public List<TagWithQuality> lookupTag(TagToken tag) {
		TagResolver tagResolver = tagResolvers.get(tag.name);
		if (tagResolver != null) {
			return tagResolver.lookup(tag);
		} else {
			return ImmutableList.of();
		}
	}

	public List<IconDefWithQuality> lookupFilter(Optional<String> type, Optional<String> name,
			Optional<String> quality) {
		type = type.or(() -> Optional.of("item"));

		if (name.isEmpty() && quality.isEmpty()) {
			return ImmutableList.of();
		}

		if (name.isPresent()) {
			return lookupSignalID(type.get(), name.get(), quality);

		} else {
			return lookupQuality(quality.get()).stream()
					.map(def -> new IconDefWithQuality(def, Optional.empty()))
					.collect(Collectors.toList());
		}
	}
}
