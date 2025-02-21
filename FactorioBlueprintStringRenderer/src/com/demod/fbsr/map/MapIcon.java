package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.demod.fbsr.Layer;

public class MapIcon extends MapRenderable {
	private static final Color SHADOW = new Color(0, 0, 0, 180);

	private final BufferedImage image;
	private final double size;
	private final double border;

	public MapIcon(MapPosition position, BufferedImage image, double size, double border) {
		super(Layer.ENTITY_INFO_ICON_ABOVE, position);
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

		AffineTransform pat = g.getTransform();
		g.translate(x - halfSize, y - halfSize);
		g.scale(size, size);
		g.drawImage(image, 0, 0, 1, 1, 0, 0, image.getWidth(), image.getHeight(), null);
		g.setTransform(pat);
	}

}
