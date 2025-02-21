package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapFoundationGrid extends MapRenderable {
	private static final BasicStroke STROKE = new BasicStroke((float) (3 / FBSR.TILE_SIZE));
	public static final Font FONT = GUIStyle.FONT_BP_REGULAR.deriveFont(0.6f);

	private final List<MapTile> tiles;
	private final Color color;

	public MapFoundationGrid(List<MapTile> tiles, Color color, boolean aboveBelts) {
		super(aboveBelts ? Layer.GRID_ABOVE_BELTS : Layer.GRID, tiles.get(0).getPosition());
		this.tiles = tiles;
		this.color = color;
	}

	@Override
	public void render(Graphics2D g) {
		g.setStroke(STROKE);
		g.setColor(color);
		Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 1, 1);
		for (MapTile tile : tiles) {
			MapPosition pos = tile.getPosition();
			rect.x = pos.getX();
			rect.y = pos.getY();
			g.draw(rect);
		}
	}

}
