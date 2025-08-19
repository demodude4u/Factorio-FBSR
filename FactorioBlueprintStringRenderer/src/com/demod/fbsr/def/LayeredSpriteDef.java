package com.demod.fbsr.def;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Optional;

import com.demod.fbsr.BlendMode;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Profile;
import com.demod.fbsr.map.MapRect;

public class LayeredSpriteDef extends SpriteDef {

	private final Layer layer;

	public LayeredSpriteDef(Profile profile, String path, ImageSheetLoader loader, Layer layer, boolean shadow, BlendMode blendMode,
			Optional<Color> tint, boolean tintAsOverlay, boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(profile, path, loader, shadow, blendMode, tint, tintAsOverlay, applyRuntimeTint, source, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(Profile profile, String path, Layer layer, boolean shadow, BlendMode blendMode, Optional<Color> tint,
			boolean tintAsOverlay, boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(profile, path, shadow, blendMode, tint, tintAsOverlay, applyRuntimeTint, source, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, BlendMode blendMode, Optional<Color> tint,
			boolean tintAsOverlay, boolean applyRuntimeTint, MapRect bounds) {
		super(shared, blendMode, tint, tintAsOverlay, applyRuntimeTint, bounds);
		this.layer = layer;
	}

	public LayeredSpriteDef(ImageDef shared, Layer layer, MapRect bounds) {
		super(shared, BlendMode.NORMAL, Optional.empty(), false, false, bounds);
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
