package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;

import com.demod.fbsr.map.MapRect;

public class LayeredSpriteDef extends SpriteDef {

	private final Layer layer;

	public LayeredSpriteDef(String path, Layer layer, boolean shadow, String blendMode, Color tint, Rectangle source,
			MapRect bounds) {
		super(path, shadow, blendMode, tint, source, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(SpriteDef shared, Layer layer) {
		super(shared);
		this.layer = layer;
	}

	public Layer getLayer() {
		return layer;
	}
}
