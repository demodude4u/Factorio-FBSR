package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Optional;

import com.demod.fbsr.map.MapRect;

public class LayeredSpriteDef extends SpriteDef {

	private final Layer layer;

	public LayeredSpriteDef(String path, ImageSheetLoader loader, Layer layer, boolean shadow, BlendMode blendMode,
			Optional<Color> tint, boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(path, loader, shadow, blendMode, tint, applyRuntimeTint, source, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(String path, Layer layer, boolean shadow, BlendMode blendMode, Optional<Color> tint,
			boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(path, shadow, blendMode, tint, applyRuntimeTint, source, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, BlendMode blendMode, Optional<Color> tint,
			boolean applyRuntimeTint, MapRect bounds) {
		super(shared, blendMode, tint, applyRuntimeTint, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, MapRect bounds) {
		super(shared, BlendMode.NORMAL, Optional.empty(), false, bounds);
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
