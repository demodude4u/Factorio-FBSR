package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;

public class MapDebugTilePlacement extends MapRenderable {

	public MapDebugTilePlacement(MapTile tile) {
		super(Layer.DEBUG_P, tile.fromBlueprint().position.createPoint());
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();

		double x = position.getX();
		double y = position.getY();

		g.setColor(Color.yellow);
		g.setStroke(new BasicStroke(1f / (float) FBSR.TILE_SIZE));
		g.draw(new Rectangle2D.Double(x - 0.5, y - 0.5, 1.0, 1.0));
		g.fill(new Ellipse2D.Double(x - 0.1, y - 0.1, 0.2, 0.2));

		g.setStroke(ps);
	}

}
