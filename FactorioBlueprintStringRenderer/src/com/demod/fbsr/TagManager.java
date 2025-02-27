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

public class TagManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagManager.class);

	public static final int DEFAULT_ICON_SIZE = 64;
	public static final String EMPTY_PATH = "__core__/graphics/empty.png";

	public static class DataPrototypeResolver implements TagResolver {
		private final List<? extends DataPrototype> list;
		private final Map<String, DataPrototype> lookup;

		public DataPrototypeResolver(List<? extends DataPrototype> list) {
			this.list = list;
			lookup = list.stream()
					.collect(Collectors.<DataPrototype, String, DataPrototype>toMap(p -> p.getName(), p -> p));
		}

		@Override
		public Optional<BufferedImage> lookup(String key) {
			DataPrototype proto = lookup.get(key);
			if (proto == null) {
				return Optional.empty();
			}
			return Optional.of(proto.getTable().getData().getWikiIcon(proto));
		}

		@Override
		public void loadDefs() {
			for (DataPrototype proto : list) {

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

	private Function<String, ? extends DataPrototype> lookupCustom(String type) {
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

	// Based on getWikiIcon()
	public static ImageDef defineIcon(DataPrototype prototype) {
		LuaValue iconLua = prototype.lua().get("icon");
		if (!iconLua.isnil()) {
			int iconSize = prototype.lua().get("icon_size").optint(DEFAULT_ICON_SIZE);

			String path = iconLua.tojstring();
			return new ImageDef(path, new Rectangle(iconSize, iconSize));
		}
		LuaValue iconsLua = prototype.lua().get("icons");

		if (iconsLua.isnil()) {
			LOGGER.error("{} ({}) has no icon.", prototype.getName(), prototype.getType());
			return new ImageDef(EMPTY_PATH, new Rectangle(DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE));
		}

		int sizeOfFirstLayer = iconsLua.get(1).get("icon_size").optint(DEFAULT_ICON_SIZE);

		BufferedImage icon = new BufferedImage(sizeOfFirstLayer, sizeOfFirstLayer, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		AffineTransform pat = g.getTransform();
		Utils.forEach(iconsLua.totableArray(), l -> {
			BufferedImage layer = getModImage(l.get("icon").tojstring());
			int layerIconSize = l.get("icon_size").optint(defaultIconSize);
			// TODO skip this call if layer.getWidth() == layerIconSize
			layer = layer.getSubimage(0, 0, layerIconSize, layerIconSize);

			LuaValue tintLua = l.get("tint");
			if (!tintLua.isnil()) {
				layer = Utils.tintImage(layer, Utils.parseColor(tintLua));
			}

			int expectedSize = 32; // items and recipes (and most other things)
			if (prototype.lua().get("type").checkjstring().equals("technology"))
				expectedSize = 128;

			/*
			 * All vanilla item and recipe icons are defined with icon size 64 (technologies
			 * with 256). However, the game "expects" icons to have a size of 32 (or 128 for
			 * technologies). Because these sizes differ, we observe the behavior that the
			 * game does not apply shift and scale values directly. Instead, shift and scale
			 * are multiplied by real_size / expected_size. In the case of items case, that
			 * means we have to multiply them by 2, because 64 / 32 = 2; this value is
			 * represented by the below variable.
			 */
			int scaleAndShiftScaling = sizeOfFirstLayer / expectedSize;

			double scale = l.get("scale").optdouble(1.0);
			// scale has to be multiplied by scaleAndShiftScaling, see above
			if (!l.get("scale").isnil()) // but only if it was defined
				scale *= scaleAndShiftScaling;

			// move icon into the center
			g.translate((icon.getWidth() / 2) - (layer.getWidth() * (scale)) / 2,
					(icon.getHeight() / 2) - (layer.getHeight() * (scale)) / 2);

			Point shift = Utils.parsePoint(l.get("shift"));
			// shift has to be multiplied by scaleAndShiftScaling, see above
			shift.x *= scaleAndShiftScaling;
			shift.y *= scaleAndShiftScaling;
			g.translate(shift.x, shift.y);

			// HACK
			// Overlay icon of equipment technology icons are outside bounds of base icon.
			// So, move the overlay icon up. Do the same for mining productivity tech.
			String path = l.get("icon").tojstring();
			if (path.equals("__core__/graphics/icons/technology/constants/constant-mining-productivity.png")) {
				g.translate(-8, -7);
			} else if (path.equals("__core__/graphics/icons/technology/constants/constant-equipment.png")) {
				g.translate(0, -20);
			}

			g.scale(scale, scale);
			g.drawImage(layer, 0, 0, null);
			g.setTransform(pat);
		});
		g.dispose();
		return icon;
	}

}
