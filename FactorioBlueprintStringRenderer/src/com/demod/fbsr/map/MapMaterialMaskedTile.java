package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Optional;

import com.demod.fbsr.Atlas;
import com.demod.fbsr.Atlas.AtlasRef;
import com.demod.fbsr.composite.MaskedTextureComposite;
import com.demod.fbsr.composite.TintMaskedTextureComposite;
import com.demod.fbsr.def.MaterialDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.Layer;

public class MapMaterialMaskedTile extends MapRenderable implements MapBounded {

	private final MaterialDef material;
	private final int row;
	private final int col;

	private final MapRect bounds;
	private final SpriteDef mask;
	private final Optional<Color> tint;

	public MapMaterialMaskedTile(MaterialDef material, SpriteDef mask, int row, int col, MapPosition pos, Optional<Color> tint) {
		super(Layer.DECALS);
		this.material = material;
		this.mask = mask;
		this.tint = tint;

		int rows = material.getRows();
		int cols = material.getCols();
		this.row = ((row % rows) + rows) % rows;
		this.col = ((col % cols) + cols) % cols;

		material.checkValid();
		mask.checkValid();

		bounds = MapRect.byUnit(0, 0, 1, 1).add(pos);
	}

	@Override
	public void render(Graphics2D g) {
		AtlasRef refMaterial = material.getAtlasRef();
		Rectangle sourceMaterial = refMaterial.getRect();

		Dimension tile = material.getTile();
		Point offsetMaterial = new Point(sourceMaterial.x + tile.width * col, sourceMaterial.y + tile.height * row);

		BufferedImage compositeImage = generateCompositeImage(offsetMaterial);

		AffineTransform pat = g.getTransform();

		// TODO change the approach to eliminate transforming on every sprite
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(compositeImage, 0, 0, 1, 1, 0, 0, compositeImage.getWidth(), compositeImage.getHeight(), null);

		g.setTransform(pat);
	}

	private BufferedImage generateCompositeImage(Point offsetMaterial) {
		AtlasRef maskRef = mask.getAtlasRef();
		Atlas maskAtlas = maskRef.getAtlas();
		Rectangle maskRect = maskRef.getRect();

		BufferedImage ret = new BufferedImage(maskRect.width, maskRect.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = ret.createGraphics();

		g.drawImage(maskAtlas.getImage(), //
				0, //
				0, //
				maskRect.width, //
				maskRect.height, //
				maskRect.x, //
				maskRect.y, //
				maskRect.x + maskRect.width, //
				maskRect.y + maskRect.height, //
				null);

		Atlas materialAtlas = material.getAtlasRef().getAtlas();
		if (tint.isPresent()) {
			g.setComposite(new TintMaskedTextureComposite(tint.get()));
		} else {
			g.setComposite(new MaskedTextureComposite());
		}
		g.drawImage(materialAtlas.getImage(), //
				0, //
				0, //
				maskRect.width, //
				maskRect.height, //
				offsetMaterial.x, //
				offsetMaterial.y, //
				offsetMaterial.x + maskRect.width, //
				offsetMaterial.y + maskRect.height, //
				null);

		g.dispose();
		return ret;
	}

	@Override
	public MapRect getBounds() {
		return bounds;
	}
}
