package com.demod.fbsr.bs;

import java.awt.geom.Point2D;

import org.json.JSONArray;
import org.json.JSONObject;

public class BSPosition {
	public static BSPosition parse(Object object /* JSONObject or JSONArray */) {
		if (object instanceof JSONObject) {
			return new BSPosition((JSONObject) object);
		} else if (object instanceof JSONArray) {
			return new BSPosition((JSONArray) object);
		} else {
			throw new IllegalArgumentException("Expected JSONObject or JSONArray!");
		}
	}

	public final double x;

	public final double y;

	public BSPosition(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public BSPosition(JSONArray json) {
		x = json.getDouble(0);
		y = json.getDouble(1);
	}

	public BSPosition(JSONObject json) {
		x = json.getDouble("x");
		y = json.getDouble("y");
	}

	public Point2D.Double createPoint() {
		return new Point2D.Double(x, y);
	}

	public Point2D.Double createPoint(Point2D.Double shift) {
		return new Point2D.Double(x + shift.x, y + shift.y);
	}
}
