package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.FactorioManager.LookupDataRawResult;
import com.google.common.collect.ImmutableList;

public class TagManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagManager.class);

	public static class DataPrototypeResolver implements TagResolver {
		private final List<? extends DataPrototype> list;
		private final Map<String, BufferedImage> lookup;

		public DataPrototypeResolver(List<? extends DataPrototype> list) {
			this.list = list;
			lookup = list.stream()
					.collect(Collectors.<DataPrototype, String, DataPrototype>toMap(p -> p.getName(), p -> p));
		}

		@Override
		public Optional<BufferedImage> lookup(String key) {
			BufferedImage image = lookup.get(key);
			if (image == null) {
				return Optional.empty();
			}
			return Optional.of(image);
		}

		@Override
		public void loadDefs() {
			for (DataPrototype proto : list) {
				//TODO
				asfasf
			}
		}
	}

	public static class DataRawResolver implements TagResolver {

		private final String[] path;

		public DataRawResolver(String... path) {
			this.path = path;
		}

		@Override
		public Optional<BufferedImage> lookup(String key) {
			Optional<LookupDataRawResult> result = FactorioManager.lookupDataRaw(path, key);
			if (!result.isPresent()) {
				return Optional.empty();
			}
			return Optional.of(result.get().data.getWikiIcon(result.get().value));
		}
	}

	public interface TagResolver {
		Optional<BufferedImage> lookup(String key);

		void loadDefs();
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

	private Function<String, ? extends DataPrototype> lookupCustomType(String type) {
		return name -> {

		};
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		// Details found at https://wiki.factorio.com/rich_text

		// TODO img
		resolvers.put("item", new DataPrototypeResolver(FactorioManager.getItems()));
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

		// Not in factorio, but still handy to have
		resolvers.put("utility-sprites", new DataRawResolver("utility-sprites"));

		// TODO preload all icons
	}
}
