package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Optional;

import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect;

public class SpriteDef extends ImageDef {
	protected final boolean shadow;
	protected final BlendMode blendMode;
	protected final Optional<Color> tint;
	protected boolean applyRuntimeTint;
	protected MapRect bounds;

	public SpriteDef(String path, boolean shadow, BlendMode blendMode, Optional<Color> tint, boolean applyRuntimeTint,
			Rectangle source, MapRect bounds) {
		super(path, source);
		this.shadow = shadow;
		this.blendMode = blendMode;
		this.tint = tint;
		this.applyRuntimeTint = applyRuntimeTint;
		this.bounds = bounds;
	}

	public SpriteDef(ImageDef shared, boolean shadow, BlendMode blendMode, Optional<Color> tint,
			boolean applyRuntimeTint, MapRect bounds) {
		super(shared);
		this.shadow = shadow;
		this.blendMode = blendMode;
		this.tint = tint;
		this.applyRuntimeTint = applyRuntimeTint;
		this.bounds = bounds;
	}

	protected SpriteDef(SpriteDef shared) {
		super(shared);
		shadow = shared.shadow;
		blendMode = shared.blendMode;
		tint = shared.tint;
		applyRuntimeTint = shared.applyRuntimeTint;
		bounds = shared.bounds;
	}

	public boolean isShadow() {
		return shadow;
	}

	public BlendMode getBlendMode() {
		return blendMode;
	}

	public boolean applyRuntimeTint() {
		return applyRuntimeTint;
	}

	public Optional<Color> getTint() {
		return tint;
	}

	public MapRect getBounds() {
		return bounds;
	}

	public void setBounds(MapRect bounds) {
		this.bounds = bounds;
	}

	public void offset(MapPosition offset) {
		this.bounds = bounds.add(offset);
	}

	public static SpriteDef fromFP(String filename, boolean shadow, BlendMode blendMode, Optional<FPColor> tint,
			boolean applyRuntimeTint, int srcX, int srcY, int srcWidth, int srcHeight, double dstX, double dstY,
			double dstScale) {
		Rectangle source = new Rectangle(srcX, srcY, srcWidth, srcHeight);
		double scaledWidth = dstScale * srcWidth / FBSR.TILE_SIZE;
		double scaledHeight = dstScale * srcHeight / FBSR.TILE_SIZE;
		MapRect bounds = MapRect.byUnit(dstX - scaledWidth / 2.0, dstY - scaledHeight / 2.0, scaledWidth, scaledHeight);
		return new SpriteDef(filename, shadow, blendMode, tint.map(FPColor::createColor), applyRuntimeTint, source,
				bounds);
	}
}
