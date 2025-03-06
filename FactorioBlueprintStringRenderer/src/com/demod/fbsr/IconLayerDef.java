package com.demod.fbsr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.fp.FPVector;
import com.google.common.collect.ImmutableList;

public class IconLayerDef extends ImageDef {
	private static final Logger LOGGER = LoggerFactory.getLogger(IconLayerDef.class);

	private final int iconSize;
	private final Color tint;
	private final Point2D.Double shift;
	private final double scale;
	private final boolean drawBackground;

	// For icon details -- https://lua-api.factorio.com/stable/types/IconData.html

	public IconLayerDef(String path, int iconSize, Color tint, Point2D.Double shift, double scale,
			boolean drawBackground) {
		super(path, new Rectangle(iconSize, iconSize));
		this.iconSize = iconSize;
		this.tint = tint;
		this.shift = shift;
		this.scale = scale;
		this.drawBackground = drawBackground;
	}

	public int getIconSize() {
		return iconSize;
	}

	public Color getTint() {
		return tint;
	}

	public Point2D.Double getShift() {
		return shift;
	}

	public double getScale() {
		return scale;
	}

	public boolean isDrawBackground() {
		return drawBackground;
	}

	public static List<IconLayerDef> fromPrototype(LuaTable lua) {
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
			return ImmutableList
					.of(new IconLayerDef(path, iconSize, Color.white, new Point2D.Double(), defaultScale, true));
		}
		LuaValue iconsLua = lua.get("icons");

		if (!iconsLua.isnil()) {
			List<IconLayerDef> defs = new ArrayList<>();
			for (int i = 1; i <= iconsLua.length(); i++) {
				LuaValue layer = iconsLua.get(i);
				String path = layer.get("icon").checkjstring();
				if (path == null) {
					throw new RuntimeException("No Icon Path!");
				}
				iconSize = layer.get("icon_size").optint(64);
				Color tint = FPUtils.<FPColor>opt(layer.get("tint"), FPColor::new).orElse(new FPColor(1, 1, 1, 1))
						.createColorIgnorePreMultipliedAlpha();
				defaultScale = (expectedIconSize / 2) / (double) iconSize;
				double scale = layer.get("scale").optdouble(defaultScale);
				Point2D.Double shift = FPUtils.<FPVector>opt(layer.get("shift"), FPVector::new)
						.orElse(new FPVector(0, 0)).createPoint();
				shift.x *= defaultScale * 2;
				shift.y *= defaultScale * 2;
				boolean drawBackground = layer.get("draw_background").optboolean(i == 0);
				defs.add(new IconLayerDef(path, iconSize, tint, shift, scale, drawBackground));
			}
			if (defs.isEmpty()) {
				throw new RuntimeException("No Icon Layers!");
			}
			return defs;
		}

//		LOGGER.error("{} ({}) has no icon.", name, type);
		return ImmutableList.of(new IconLayerDef("__core__/graphics/empty.png", iconSize, Color.white,
				new Point2D.Double(), defaultScale, true));
	}

	public static BufferedImage createIcon(List<IconLayerDef> defs) {
		int sizeOfFirstLayer = defs.get(0).getIconSize();

		BufferedImage icon = new BufferedImage(sizeOfFirstLayer, sizeOfFirstLayer, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		AffineTransform pat = g.getTransform();
		for (IconLayerDef def : defs) {
			AtlasRef ref = def.atlasRef;
			BufferedImage srcImage = ref.getAtlas().getBufferedImage();
			Rectangle rect = ref.getRect();

			BufferedImage layer = srcImage.getSubimage(rect.x, rect.y, rect.width, rect.height);

			if (!def.tint.equals(Color.white)) {
				layer = Utils.tintImage(layer, def.tint);
			}

			// move icon into the center
			g.translate((icon.getWidth() / 2) - (layer.getWidth() * (def.scale)) / 2,
					(icon.getHeight() / 2) - (layer.getHeight() * (def.scale)) / 2);

			g.translate(def.shift.x, def.shift.y);

			// TODO how do I deal with this?
			// HACK
			// Overlay icon of equipment technology icons are outside bounds of base icon.
			// So, move the overlay icon up. Do the same for mining productivity tech.
//			String path = l.get("icon").tojstring();
//			if (path.equals("__core__/graphics/icons/technology/constants/constant-mining-productivity.png")) {
//				g.translate(-8, -7);
//			} else if (path.equals("__core__/graphics/icons/technology/constants/constant-equipment.png")) {
//				g.translate(0, -20);
//			}

			g.scale(def.scale, def.scale);
			g.drawImage(layer, 0, 0, null);
			g.setTransform(pat);
		}
		g.dispose();
		return icon;
	}

}
