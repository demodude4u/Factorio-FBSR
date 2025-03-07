package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;

public class MapIcon extends MapRenderable {
	private static final Color SHADOW = new Color(0, 0, 0, 180);

	private final MapPosition position;
	private final ImageDef image;
	private final double size;
	private final double border;

	public MapIcon(MapPosition position, ImageDef image, double size, double border, boolean above) {
		super(above ? Layer.ENTITY_INFO_ICON_ABOVE : Layer.ENTITY_INFO_ICON);
		this.position = position;
		this.image = image;
		this.size = size;
		this.border = border;
	}

	@Override
	public void render(Graphics2D g) {
		double x = position.getX();
		double y = position.getY();
		double halfSize = size / 2.0;
		double shadowSize = size + border * 2.0;
		double halfShadowSize = shadowSize / 2.0;

		g.setColor(SHADOW);
		g.fill(new Rectangle2D.Double(x - halfShadowSize, y - halfShadowSize, shadowSize, shadowSize));

		AtlasRef ref = image.getAtlasRef();
		if (!ref.isValid()) {
			throw new IllegalStateException("Icon not assigned to atlas! " + image.getPath());
		}
		Image image = ref.getAtlas().getBufferedImage();
		Rectangle source = ref.getRect();

		AffineTransform pat = g.getTransform();

		MapRect bounds = MapRect.byUnit(x - halfSize, y - halfSize, size, size);
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

		g.setTransform(pat);
	}

}
