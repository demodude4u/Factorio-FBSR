package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import com.demod.fbsr.Layer;

public class MapWireShadow extends MapRenderable {
	public static final double DROP = 0.6;

	private final MapPosition p1;
	private final MapPosition p2;

	public MapWireShadow(MapPosition p1, MapPosition p2) {
		super(Layer.SHADOW_BUFFER);
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();
		g.setStroke(new BasicStroke(1f / 48f));
		g.setColor(Color.black);

		double x1 = p1.getX();
		double y1 = p1.getY();
		double x2 = p2.getX();
		double y2 = p2.getY();
		Path2D.Double path = new Path2D.Double();
		path.moveTo(x1, y1);
		Point2D.Double mid = new Point2D.Double((x1 + x2) / 2.0 - DROP, (y1 + y2) / 2.0);
		path.curveTo(mid.x, mid.y, mid.x, mid.y, x2, y2);
		g.draw(path);

		g.setStroke(ps);
	}

}
