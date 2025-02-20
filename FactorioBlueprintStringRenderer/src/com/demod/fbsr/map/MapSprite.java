package com.demod.fbsr.map;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.demod.fbsr.Layer;

public class MapSprite extends MapRenderable {

	private final BufferedImage image;
	private final Rectangle source;
	private final MapRect bounds;

	public MapSprite(Layer layer, BufferedImage image, Rectangle source, MapRect bounds) {
		super(layer, bounds.getTopLeft());
		this.image = image;
		this.source = source;
		this.bounds = bounds;
	}

	public MapRect getBounds() {
		return bounds;
	}

	public BufferedImage getImage() {
		return image;
	}

	public Rectangle getSource() {
		return source;
	}

	// TODO change the approach to eliminate transforming on every sprite
	@Override
	public void render(Graphics2D g) {
		AffineTransform pat = g.getTransform();
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);
		g.setTransform(pat);
	}
}
