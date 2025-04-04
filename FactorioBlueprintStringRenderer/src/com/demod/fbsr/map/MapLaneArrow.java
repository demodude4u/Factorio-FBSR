package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;

public class MapLaneArrow extends MapRenderable {
	private static final Path2D.Double markerShape = new Path2D.Double();
	static {
		markerShape.moveTo(-0.5 + 0.2, 0.5 - 0.125);
		markerShape.lineTo(0.5 - 0.2, 0.5 - 0.125);
		markerShape.lineTo(0, 0 + 0.125);
		markerShape.closePath();
	}

	private final MapPosition pos;
	private final Direction dir;

	public MapLaneArrow(MapPosition pos, Direction dir) {
		super(Layer.ENTITY_INFO_ICON_ABOVE);
		this.pos = pos;
		this.dir = dir;
	}

	@Override
	public void render(Graphics2D g) {
		AffineTransform pat = g.getTransform();

		Color color = Color.yellow;
		Color shadow = Color.darkGray;
		double shadowShift = 0.07;

		g.setTransform(pat);
		g.translate(pos.getX(), pos.getY());
		g.rotate(dir.back().ordinal() * Math.PI / 4.0 + Math.PI);
		g.translate(shadowShift, shadowShift);
		g.setColor(shadow);
		g.fill(markerShape);

		g.setTransform(pat);
		g.translate(pos.getX(), pos.getY());
		g.rotate(dir.back().ordinal() * Math.PI / 4.0 + Math.PI);
		g.setColor(color);
		g.fill(markerShape);

		g.setTransform(pat);
	}

}
