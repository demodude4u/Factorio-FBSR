package com.demod.fbsr;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Optional;

import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect;

public class SpriteDef extends ImageDef {
	protected final BlendMode blendMode;
	protected final Optional<Color> tint;
	protected boolean applyRuntimeTint;
	protected MapRect sourceBounds;
	private MapRect trimmedBounds;

	public SpriteDef(String path, ImageSheetLoader loader, boolean shadow, BlendMode blendMode, Optional<Color> tint,
			boolean applyRuntimeTint, Rectangle source, MapRect bounds) {
		super(path, loader, source, shadow);
		this.blendMode = blendMode;
		this.tint = tint;
		this.applyRuntimeTint = applyRuntimeTint;
		this.sourceBounds = bounds;
	}

	public SpriteDef(String path, boolean shadow, BlendMode blendMode, Optional<Color> tint, boolean applyRuntimeTint,
			Rectangle source, MapRect bounds) {
		super(path, source, shadow);
		this.blendMode = blendMode;
		this.tint = tint;
		this.applyRuntimeTint = applyRuntimeTint;
		this.sourceBounds = bounds;
	}

	public SpriteDef(ImageDef shared, BlendMode blendMode, Optional<Color> tint, boolean applyRuntimeTint,
			MapRect bounds) {
		super(shared);
		this.blendMode = blendMode;
		this.tint = tint;
		this.applyRuntimeTint = applyRuntimeTint;
		this.sourceBounds = bounds;
		updateTrimmedBounds();
	}

	protected SpriteDef(SpriteDef shared) {
		super(shared);
		blendMode = shared.blendMode;
		tint = shared.tint;
		applyRuntimeTint = shared.applyRuntimeTint;
		sourceBounds = shared.sourceBounds;
		trimmedBounds = shared.trimmedBounds;
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

	public MapRect getSourceBounds() {
		return sourceBounds;
	}

	public MapRect getTrimmedBounds() {
		if (trimmedBounds == null) {
			updateTrimmedBounds();
		}
		return trimmedBounds;
	}

	public void setSourceBounds(MapRect bounds) {
		this.sourceBounds = bounds;
		updateTrimmedBounds();
	}

	public void offset(MapPosition offset) {
		this.sourceBounds = sourceBounds.add(offset);
		if (trimmedBounds != null) {
			this.trimmedBounds = trimmedBounds.add(offset);
		}
	}

	@Override
	public void setTrimmed(Rectangle trimmed) {
		super.setTrimmed(trimmed);
		updateTrimmedBounds();
	}

	private void updateTrimmedBounds() {
		Rectangle trimmed = getTrimmed();
		if (trimmed != null) {
			double x = sourceBounds.getX() + sourceBounds.getWidth() * ((trimmed.x - source.x) / (double) source.width);
			double y = sourceBounds.getY()
					+ sourceBounds.getHeight() * ((trimmed.y - source.y) / (double) source.height);
			double width = sourceBounds.getWidth() * (trimmed.width / (double) source.width);
			double height = sourceBounds.getHeight() * (trimmed.height / (double) source.height);
			trimmedBounds = MapRect.byUnit(x, y, width, height);
		}
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
