package com.demod.fbsr.map;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.demod.fbsr.Layer;

public class MapSprite extends MapRender {

	public MapSprite(Layer layer, BufferedImage image, Rectangle source, MapPosition position) {
		super(layer, position);
		// TODO Auto-generated constructor stub
	}

	// TODO change the approach to eliminate transforming on every sprite
	@Override
	public void render(Graphics2D g) {
		AffineTransform pat = g.getTransform();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width, bounds.height);
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);
		g.setTransform(pat);
	}

}
