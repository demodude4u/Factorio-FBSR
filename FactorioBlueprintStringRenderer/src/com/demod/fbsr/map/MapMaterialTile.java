package com.demod.fbsr.map;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.def.MaterialDef;
import com.demod.fbsr.Layer;

public class MapMaterialTile extends MapRenderable implements MapBounded {

	private final MaterialDef def;
	private final int row;
	private final int col;

	private final MapRect bounds;

	public MapMaterialTile(MaterialDef def, int row, int col, MapPosition pos) {
		super(Layer.DECALS);
		this.def = def;

		int rows = def.getRows();
		int cols = def.getCols();
		this.row = ((row % rows) + rows) % rows;
		this.col = ((col % cols) + cols) % cols;

		def.checkValid();

		bounds = MapRect.byUnit(0, 0, 1, 1).add(pos);
	}

	@Override
	public void render(Graphics2D g) {
		AtlasRef ref = def.getAtlasRef();
		Image image = ref.getAtlas().getBufferedImage();
		Rectangle source = ref.getRect();

		Dimension tile = def.getTile();
		Point offset = new Point(source.x + tile.width * col, source.y + tile.height * row);

		AffineTransform pat = g.getTransform();

		// TODO change the approach to eliminate transforming on every sprite
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, offset.x, offset.y, offset.x + tile.width, offset.y + tile.height, null);

		g.setTransform(pat);
	}

	@Override
	public MapRect getBounds() {
		return bounds;
	}
}
