package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;

import com.demod.fbsr.map.MapRect;

public class SpriteDef {
	private final String path;
	private final boolean shadow;
	private final String blendMode;
	private final Color tint;
	private final Rectangle source;
	private final MapRect bounds;

	public SpriteDef(String path, boolean shadow, String blendMode, Color tint, Rectangle source, MapRect bounds) {
		this.path = path;
		this.shadow = shadow;
		this.blendMode = blendMode;
		this.tint = tint;
		this.source = source;
		this.bounds = bounds;
	}

	public String getPath() {
		return path;
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

	public Rectangle getSource() {
		return source;
	}

	public MapRect getBounds() {
		return bounds;
	}

	public LayeredSpriteDef withLayer(Layer layer) {
		return new LayeredSpriteDef(path, layer, shadow, blendMode, tint, source, bounds);
	}
}
