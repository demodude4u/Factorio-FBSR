package com.demod.fbsr;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.prototype.DataPrototype;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

public class TagManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagManager.class);

	public static final int ICON_SIZE = 64;

	public static class DefaultResolver extends TagResolver {
		private final Map<String, LuaTable> map;

		private final ListMultimap<String, IconLayer> layers;
		private final Map<String, ImageDef> defs;

		public DefaultResolver(String key, Map<String, LuaTable> map) {
			super(key);

			this.map = map;

			layers = createLayers();
			defs = createDefs();
		}

		private ListMultimap<String, IconLayer> createLayers() {
			ListMultimap<String, IconLayer> layers = MultimapBuilder.hashKeys().arrayListValues().build();
			for (Entry<String, LuaTable> entry : map.entrySet()) {
				layers.putAll(entry.getKey(), IconLayer.fromPrototype(entry.getValue()));
			}
			return layers;
		}

		private Map<String, ImageDef> createDefs() {
			Map<String, ImageDef> defs = new HashMap<>();
			for (Entry<String, List<IconLayer>> entry : Multimaps.asMap(layers).entrySet()) {
				ImageDef def = new ImageDef("TAG[" + key + "]/" + entry.getKey() + "/" + ICON_SIZE, k -> {
					return IconLayer.createIcon(layers.get(entry.getKey()), ICON_SIZE);
				}, new Rectangle(ICON_SIZE, ICON_SIZE));
				def.setTrimmable(false);
				defs.put(entry.getKey(), def);
			}
			return defs;
		}

		@Override
		public Optional<ImageDef> lookup(String key) {
			return Optional.ofNullable(defs.get(key));
		}

		@Override
		public Optional<List<IconLayer>> lookupLayers(String key) {
			return Optional.ofNullable(layers.get(key));
		}

		@Override
		public void getDefs(Consumer<ImageDef> register) {
			defs.values().forEach(register);
		}

	}

	public static abstract class TagResolver {
		protected final String key;

		public TagResolver(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public abstract Optional<ImageDef> lookup(String key);

		public abstract Optional<List<IconLayer>> lookupLayers(String key);

		public abstract void getDefs(Consumer<ImageDef> register);

		public static void forMap(String key, Map<String, LuaTable> map) {
			resolvers.put(key, new DefaultResolver(key, map));
		}

		public static void forPrototypes(String key, List<? extends DataPrototype> prototypes) {
			Map<String, LuaTable> map = new LinkedHashMap<>();
			for (DataPrototype proto : prototypes) {
				map.put(proto.getName(), proto.lua());
			}
			resolvers.put(key, new DefaultResolver(key, map));
		}

		public static void forPath(String key, String rawPath) {
			String[] path = rawPath.split("\\.");
			Map<String, LuaTable> map = new LinkedHashMap<>();
			for (FactorioData data : FactorioManager.getDatas()) {
				LuaTable lua = data.getTable().getRaw(path).get().totableObject();
				Utils.forEach(lua, (k, v) -> {
					map.put(k.tojstring(), v.totableObject());
				});
			}
			resolvers.put(key, new DefaultResolver(key, map));
		}
	}

	private static Map<String, TagResolver> resolvers = new HashMap<>();

	private static volatile boolean initialized = false;

//	public static Optional<BufferedImage> getIconForSignal(String name) {
//		// TODO
//	}
//
//	public static TaggedText getRichTextForString(String string) {
//		// TODO
//	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		// Details found at https://wiki.factorio.com/rich_text

		// TODO img
		TagResolver.forPrototypes("item", FactorioManager.getItems());
		TagResolver.forPrototypes("entity", FactorioManager.getEntities());
		TagResolver.forPrototypes("technology", FactorioManager.getTechnologies());
		TagResolver.forPrototypes("recipe", FactorioManager.getRecipes());
		TagResolver.forPath("item-group", "item-group");
		TagResolver.forPrototypes("fluid", FactorioManager.getFluids());
		TagResolver.forPrototypes("tile", FactorioManager.getTiles());
		TagResolver.forPath("virtual-signal", "virtual-signal");
		TagResolver.forPrototypes("achievement", FactorioManager.getAchievements());
		// TODO gps
		// TODO special-item
		TagResolver.forPath("armor", "armor");
		// TODO train
		// TODO train-stop
		TagResolver.forPath("shortcut", "shortcut");
		// TODO tip
		// TODO tooltip
		TagResolver.forPath("quality", "quality");
		// TODO space-platform
		TagResolver.forPath("planet", "planet");
		TagResolver.forPath("space-location", "space-location");
		// TODO space-age

		resolvers.values().forEach(r -> r.getDefs(AtlasManager::registerDef));
	}

	public static Optional<ImageDef> lookup(String key, String name) {
		return resolvers.get(key).lookup(name);
	}
}
