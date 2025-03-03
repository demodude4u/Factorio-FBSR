package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;

import com.demod.fbsr.map.MapRect;

public class LayeredSpriteDef extends SpriteDef {

	private final Layer layer;

	public LayeredSpriteDef(String path, Layer layer, boolean shadow, String blendMode, Color tint, Rectangle source,
			MapRect bounds) {
		super(path, shadow, blendMode, tint, source, bounds);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, boolean shadow, String blendMode, Color tint,
			MapRect bounds) {
		super(shared, shadow, blendMode, tint, bounds);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, MapRect bounds) {
		super(shared, false, "normal", Color.white, bounds);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public LayeredSpriteDef(SpriteDef shared, Layer layer) {
		super(shared);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public Layer getLayer() {
		return layer;
	}
}
