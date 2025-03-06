package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Function;

import com.demod.fbsr.map.MapRect;

public class LayeredSpriteDef extends SpriteDef {

	private final Layer layer;

	public LayeredSpriteDef(String path, Function<String, BufferedImage> loader, Layer layer, boolean shadow,
			BlendMode blendMode, Optional<Color> tint, boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(path, loader, shadow, blendMode, tint, applyRuntimeTint, source, bounds);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public LayeredSpriteDef(String path, Layer layer, boolean shadow, BlendMode blendMode, Optional<Color> tint,
			boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(path, shadow, blendMode, tint, applyRuntimeTint, source, bounds);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, boolean shadow, BlendMode blendMode, Optional<Color> tint,
			boolean applyRuntimeTint, MapRect bounds) {
		super(shared, shadow, blendMode, tint, applyRuntimeTint, bounds);
		this.layer = shadow ? Layer.SHADOW_BUFFER : layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, MapRect bounds) {
		super(shared, false, BlendMode.NORMAL, Optional.empty(), false, bounds);
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
