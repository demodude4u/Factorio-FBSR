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

public class MapDebugEntityPlacement extends MapRenderable {

	private final Direction direction;
	private final MapRect3D bounds;

	public MapDebugEntityPlacement(MapPosition position, Direction direction, MapRect3D bounds) {
		super(Layer.DEBUG_P, position);
		this.direction = direction;
		this.bounds = bounds;
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();

		double x = position.getX();
		double y = position.getY();

		g.setStroke(new BasicStroke(1f / (float) FBSR.TILE_SIZE));
		g.setColor(Color.magenta);
		g.draw(new Rectangle2D.Double(bounds.getX1(), bounds.getY1() - bounds.getHeight(),
				bounds.getX2() - bounds.getX1(), bounds.getY2() - bounds.getY1() + bounds.getHeight()));
		if (bounds.getHeight() > 0) {
			g.setColor(g.getColor().darker());
			g.draw(new Line2D.Double(bounds.getX1(), bounds.getY1(), bounds.getX2(), bounds.getY1()));
		}

		g.setColor(Color.cyan);
		g.fill(new Ellipse2D.Double(x - 0.1, y - 0.1, 0.2, 0.2));
		g.setStroke(new BasicStroke(3f / (float) FBSR.TILE_SIZE));
		g.setColor(Color.green);
		MapPosition offset = direction.offset(position, 0.3);
		g.draw(new Line2D.Double(x, y, offset.getX(), offset.getY()));

		g.setStroke(ps);
	}

}
