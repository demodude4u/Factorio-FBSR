package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import com.demod.fbsr.Layer;

public class MapWireShadow extends MapRenderable {
	public static final float DROP = 0.6f;

	private final MapPosition p1;
	private final MapPosition p2;

	public MapWireShadow(MapPosition p1, MapPosition p2) {
		super(Layer.SHADOW_BUFFER, p1);
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();
		g.setStroke(new BasicStroke(1f / 48f));
		g.setColor(Color.black);

		float x1 = p1.getX();
		float y1 = p1.getY();
		float x2 = p2.getX();
		float y2 = p2.getY();
		Path2D.Float path = new Path2D.Float();
		path.moveTo(x1, y1);
		Point2D.Float mid = new Point2D.Float((x1 + x2) / 2 - DROP, (y1 + y2) / 2);
		path.curveTo(mid.x, mid.y, mid.x, mid.y, x2, y2);
		g.draw(path);

		g.setStroke(ps);
	}

}
