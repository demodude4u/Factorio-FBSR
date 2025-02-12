package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.demod.factorio.prototype.DataPrototype;

public class TagManager {

	public static class DataPrototypeResolver implements TagResolver {
		private final Function<String, Optional<? extends DataPrototype>> lookup;

		public DataPrototypeResolver(Function<String, Optional<? extends DataPrototype>> lookup) {
			this.lookup = lookup;
		}

		@Override
		public Optional<BufferedImage> lookup(String key) {
			Optional<? extends DataPrototype> proto = lookup.apply(key);
			if (!proto.isPresent()) {
				return Optional.empty();
			}
			return Optional.of(proto.get().getTable().getData().getWikiIcon(proto.get()));
		}
	}

//	public static class DataRawResolver implements TagResolver {
//
//		private final String[] path;
//
//		public DataRawResolver(String... path) {
//			this.path = path;
//		}
//
//		@Override
//		public Optional<BufferedImage> lookup(String key) {
//			Optional<LookupDataRawResult> result = FactorioManager.lookupDataRaw(path, key);
//			if (!result.isPresent()) {
//				return Optional.empty();
//			}
//			return Optional.of(result.get().data.getWikiIcon(result.get().value));
//		}
//	}

	@FunctionalInterface
	public interface TagResolver {
		Optional<BufferedImage> lookup(String key);
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
		resolvers.put("item", new DataPrototypeResolver(FactorioManager::lookupItemByName));
		resolvers.put("entity", new DataPrototypeResolver(FactorioManager::lookupEntityByName));
		resolvers.put("technology", new DataPrototypeResolver(FactorioManager::lookupTechnologyByName));
		resolvers.put("recipe", new DataPrototypeResolver(FactorioManager::lookupRecipeByName));
//		resolvers.put("item-group", new DataRawResolver("item-group"));
		resolvers.put("fluid", new DataPrototypeResolver(FactorioManager::lookupFluidByName));
		resolvers.put("tile", new DataPrototypeResolver(FactorioManager::lookupTileByName));
//		resolvers.put("virtual-signal", new DataRawResolver("virtual-signal"));
		// TODO achievement
		// TODO gps
		// TODO special-item
		// TODO armor
		// TODO train
		// TODO train-stop
		// TODO shortcut
		// TODO tip
		// TODO tooltip
		// TODO quality
		// TODO space-platform
		// TODO planet
		// TODO space-location
		// TODO space-age

		// TODO preload all icons
	}

}
