package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import com.demod.fbsr.Layer;

public class MapWire extends MapRenderable {
	public static final double DROP = 0.6;

	private final MapPosition p1;
	private final MapPosition p2;
	private final Color color;

	public MapWire(MapPosition p1, MapPosition p2, Color color) {
		super(Layer.WIRE, p1);
		this.p1 = p1;
		this.p2 = p2;
		this.color = color;
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();
		g.setStroke(new BasicStroke(1f / 32f));
		g.setColor(color);

		double x1 = p1.getX();
		double y1 = p1.getY();
		double x2 = p2.getX();
		double y2 = p2.getY();
		Path2D.Double path = new Path2D.Double();
		path.moveTo(x1, y1);
		Point2D.Double mid = new Point2D.Double((x1 + x2) / 2.0, (y1 + y2) / 2.0 + DROP);
		path.curveTo(mid.x, mid.y, mid.x, mid.y, x2, y2);
		g.draw(path);

		g.setStroke(ps);
	}

}
