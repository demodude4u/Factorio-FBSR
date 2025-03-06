package com.demod.fbsr;

import java.awt.image.BufferedImage;
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

public class TagManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagManager.class);

	public static class DefaultResolver implements TagResolver {
		private final Map<String, LuaTable> map;
		private final ListMultimap<String, IconLayerDef> defs;
		private final Map<String, BufferedImage> cache;

		public DefaultResolver(Map<String, LuaTable> map) {
			this.map = map;
			cache = new HashMap<>();

			defs = createDefs();
		}

		private ListMultimap<String, IconLayerDef> createDefs() {
			ListMultimap<String, IconLayerDef> defs = MultimapBuilder.hashKeys().arrayListValues().build();
			for (Entry<String, LuaTable> entry : map.entrySet()) {
				defs.putAll(entry.getKey(), IconLayerDef.fromPrototype(entry.getValue()));
			}
			return defs;
		}

		@Override
		public Optional<BufferedImage> lookup(String key) {
			BufferedImage image = cache.get(key);
			if (image != null) {
				return Optional.of(image);
			}
			List<IconLayerDef> protoDefs = defs.get(key);
			if (protoDefs.isEmpty()) {
				return Optional.empty();
			}
			image = IconLayerDef.createIcon(protoDefs);
			cache.put(key, image);
			return Optional.of(image);
		}

		@Override
		public void getDefs(Consumer<ImageDef> register) {
			defs.values().forEach(register);
		}
	}

	public interface TagResolver {
		Optional<BufferedImage> lookup(String key);

		void getDefs(Consumer<ImageDef> register);

		public static TagResolver forMap(Map<String, LuaTable> map) {
			return new DefaultResolver(map);
		}

		public static TagResolver forPrototypes(List<? extends DataPrototype> prototypes) {
			Map<String, LuaTable> map = new LinkedHashMap<>();
			for (DataPrototype proto : prototypes) {
				map.put(proto.getName(), proto.lua());
			}
			return new DefaultResolver(map);
		}

		public static TagResolver forPath(String rawPath) {
			String[] path = rawPath.split("\\.");
			Map<String, LuaTable> map = new LinkedHashMap<>();
			for (FactorioData data : FactorioManager.getDatas()) {
				LuaTable lua = data.getTable().getRaw(path).get().totableObject();
				Utils.forEach(lua, (k, v) -> {
					map.put(k.tojstring(), v.totableObject());
				});
			}
			return new DefaultResolver(map);
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
		resolvers.put("item", TagResolver.forPrototypes(FactorioManager.getItems()));
		resolvers.put("entity", TagResolver.forPrototypes(FactorioManager.getEntities()));
		resolvers.put("technology", TagResolver.forPrototypes(FactorioManager.getTechnologies()));
		resolvers.put("recipe", TagResolver.forPrototypes(FactorioManager.getRecipes()));
		resolvers.put("item-group", TagResolver.forPath("item-group"));
		resolvers.put("fluid", TagResolver.forPrototypes(FactorioManager.getFluids()));
		resolvers.put("tile", TagResolver.forPrototypes(FactorioManager.getTiles()));
		resolvers.put("virtual-signal", TagResolver.forPath("virtual-signal"));
		resolvers.put("achievement", TagResolver.forPrototypes(FactorioManager.getAchievements()));
		// TODO gps
		// TODO special-item
		resolvers.put("armor", TagResolver.forPath("armor"));
		// TODO train
		// TODO train-stop
		resolvers.put("shortcut", TagResolver.forPath("shortcut"));
		// TODO tip
		// TODO tooltip
		resolvers.put("quality", TagResolver.forPath("quality"));
		// TODO space-platform
		resolvers.put("planet", TagResolver.forPath("planet"));
		resolvers.put("space-location", TagResolver.forPath("space-location"));
		// TODO space-age

		resolvers.values().forEach(r -> r.getDefs(AtlasManager::registerDef));
	}

	public static Optional<BufferedImage> lookup(String key, String name) {
		return resolvers.get(key).lookup(name);
	}
}
