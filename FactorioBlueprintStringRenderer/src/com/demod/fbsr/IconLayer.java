package com.demod.fbsr;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.composite.TintComposite;
import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.fp.FPVector;
import com.google.common.collect.ImmutableList;

public class IconLayer {
	public static final IconLayer EMPTY_LAYER = new IconLayer("__core__/graphics/empty.png",
			new Rectangle(0, 0, 64, 64), new Rectangle2D.Double(0, 0, 1, 1), Color.white, false);

	private final String path;
	private final Rectangle source;
	private final Rectangle2D.Double bounds;
	private final Color tint;
	private final boolean drawBackground;


	// For icon details -- https://lua-api.factorio.com/stable/types/IconData.html

	public IconLayer(String path, Rectangle source, Rectangle2D.Double bounds, Color tint, boolean drawBackground) {
		this.path = path;
		this.source = source;
		this.bounds = bounds;
		this.tint = tint;
		this.drawBackground = drawBackground;
	}

	public Rectangle getSource() {
		return source;
	}

	public Rectangle2D.Double getBounds() {
		return bounds;
	}

	public Color getTint() {
		return tint;
	}

	public boolean isDrawBackground() {
		return drawBackground;
	}

	public static List<IconLayer> fromPrototype(LuaTable lua) {
		String type = lua.get("type").tojstring();

		int expectedIconSize;
		if (type.equals("technology")) {
			expectedIconSize = 256;
		} else if (type.equals("achievement")) {
			expectedIconSize = 128;
		} else if (type.equals("item-group")) {
			expectedIconSize = 128;
		} else if (type.equals("shortcut")) {
			expectedIconSize = 32;
		} else {
			expectedIconSize = 64;
		}

		int iconSize = lua.get("icon_size").optint(64);

		double defaultScale = (expectedIconSize / 2) / (double) iconSize;

		LuaValue iconLua = lua.get("icon");
		if (!iconLua.isnil()) {

			String path = iconLua.tojstring();
			if (path == null) {
				throw new RuntimeException("No Icon Path!");
			}

			Rectangle source = new Rectangle(0, 0, iconSize, iconSize);
			Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 1, 1);
			return ImmutableList.of(new IconLayer(path, source, bounds, Color.white, true));
		}
		LuaValue iconsLua = lua.get("icons");

		if (!iconsLua.isnil()) {
			List<IconLayer> layers = new ArrayList<>();
			for (int i = 1; i <= iconsLua.length(); i++) {
				LuaValue layer = iconsLua.get(i);
				String path = layer.get("icon").checkjstring();
				if (path == null) {
					throw new RuntimeException("No Icon Path!");
				}
				iconSize = layer.get("icon_size").optint(64);
				Rectangle source = new Rectangle(0, 0, iconSize, iconSize);
				Color tint = FPUtils.<FPColor>opt(layer.get("tint"), FPColor::new).orElse(new FPColor(1, 1, 1, 1))
						.createColorIgnorePreMultipliedAlpha();

				defaultScale = (expectedIconSize / 2) / (double) iconSize;
				double scale = layer.get("scale").optdouble(defaultScale) * 2;

				Rectangle2D.Double bounds = new Rectangle2D.Double(0.5 - scale / 2.0, 0.5 - scale / 2.0, scale, scale);

				Point2D.Double shift = FPUtils.<FPVector>opt(layer.get("shift"), FPVector::new)
						.orElseGet(() -> new FPVector(0, 0)).createPoint();
				bounds.x += shift.x / (expectedIconSize / 2.0);
				bounds.y += shift.y / (expectedIconSize / 2.0);

				boolean drawBackground = layer.get("draw_background").optboolean(i == 0);
				layers.add(new IconLayer(path, source, bounds, tint, drawBackground));
			}
			if (layers.isEmpty()) {
				throw new RuntimeException("No Icon Layers!");
			}
			return layers;
		}

//		LOGGER.error("{} ({}) has no icon.", name, type);
		return ImmutableList.of(EMPTY_LAYER);
	}

	public static BufferedImage createIcon(FactorioManager factorioManager, List<IconLayer> defs, int size) {

		BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		Composite pc = g.getComposite();

		for (IconLayer layer : defs) {
			BufferedImage imageSheet = factorioManager.lookupModImage(layer.path);

			if (!layer.tint.equals(Color.white)) {
				g.setComposite(new TintComposite(layer.tint));
			} else {
				g.setComposite(pc);
			}

			// TODO use drawBackground to draw a SDF outline

			g.drawImage(imageSheet, //
					(int) (layer.bounds.x * size), //
					(int) (layer.bounds.y * size), //
					(int) ((layer.bounds.x + layer.bounds.width) * size), //
					(int) ((layer.bounds.y + layer.bounds.height) * size), //
					layer.source.x, //
					layer.source.y, //
					layer.source.x + layer.source.width, //
					layer.source.y + layer.source.height, //
					null);
		}
		g.dispose();

		return icon;
	}

    public String getModName() {
        String firstSegment = path.split("\\/")[0];
		return firstSegment.substring(2, firstSegment.length() - 2);
    }
}
