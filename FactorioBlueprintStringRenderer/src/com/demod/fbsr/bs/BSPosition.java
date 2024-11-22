package com.demod.fbsr.bs;

import java.awt.geom.Point2D;

import org.json.JSONObject;

public class BSPosition {
	public final double x;
	public final double y;

	public BSPosition(JSONObject json) {
		x = json.getDouble("x");
		y = json.getDouble("y");
	}

	public Point2D.Double createPoint() {
		return new Point2D.Double(x, y);
	}
}
