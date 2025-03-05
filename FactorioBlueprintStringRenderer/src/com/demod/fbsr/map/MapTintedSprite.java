package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.SpriteDef;

public class MapTintedSprite extends MapSprite {

	private final Color tint;

	public MapTintedSprite(LayeredSpriteDef def, MapPosition pos, Color tint) {
		super(def, pos);
		this.tint = tint;
	}

	public MapTintedSprite(SpriteDef def, Layer layer, MapPosition pos, Color tint) {
		super(def, layer, pos);
		this.tint = tint;
	}

	@Override
	public void render(Graphics2D g) {
		AtlasRef ref = def.getAtlasRef();
		if (!ref.isValid()) {
			throw new IllegalStateException("Sprite not assigned to atlas! " + def.getPath());
		}
		Image image = ref.getAtlas().getVolatileImage();

		Rectangle source = ref.getRect();

		// TODO change the approach to eliminate transforming on every sprite
		AffineTransform pat = g.getTransform();
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);
		g.setTransform(pat);
	}
}
