package com.demod.fbsr.map;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.SpriteDef;

public class MapSprite extends MapRenderable {

	private final LayeredSpriteDef def;
	private final MapRect bounds;

	public MapSprite(SpriteDef def, Layer layer, MapPosition pos) {
		this(new LayeredSpriteDef(def, layer), pos);
	}

	public MapSprite(LayeredSpriteDef def, MapPosition pos) {
		super(def.getLayer(), def.getBounds().getTopLeft());
		this.def = def;
		this.bounds = def.getBounds().add(pos);
	}

	public SpriteDef getDef() {
		return def;
	}

	public MapRect getBounds() {
		return bounds;
	}

	// TODO change the approach to eliminate transforming on every sprite
	@Override
	public void render(Graphics2D g) {
		AtlasRef ref = def.getAtlasRef();
		if (!ref.isValid()) {
			throw new IllegalStateException("Sprite not assigned to atlas! " + def.getPath());
		}
		Image image = ref.getAtlas().getVolatileImage();
		Rectangle source = ref.getRect();

		AffineTransform pat = g.getTransform();
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);
		g.setTransform(pat);
	}
}
