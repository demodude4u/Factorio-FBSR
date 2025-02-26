package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;

import com.demod.fbsr.map.MapRect;

public class SpriteDef extends ImageDef {
	protected final boolean shadow;
	protected final String blendMode;
	protected final Color tint;
	protected final MapRect bounds;

	public SpriteDef(String path, boolean shadow, String blendMode, Color tint, Rectangle source, MapRect bounds) {
		super(path, source);
		this.shadow = shadow;
		this.blendMode = blendMode;
		this.tint = tint;
		this.bounds = bounds;
	}

	public boolean isShadow() {
		return shadow;
	}

	public String getBlendMode() {
		return blendMode;
	}

	public Color getTint() {
		return tint;
	}

	public MapRect getBounds() {
		return bounds;
	}

	public LayeredSpriteDef withLayer(Layer layer) {
		return new LayeredSpriteDef(path, layer, shadow, blendMode, tint, source, bounds);
	}
}
