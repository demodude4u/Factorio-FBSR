package com.demod.fbsr.bs;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.fbsr.map.MapPosition;

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

	public MapPosition createPoint() {
		return MapPosition.byUnit(x, y);
	}

	public MapPosition createPoint(MapPosition shift) {
		return shift.addUnit(x, y);
	}
}
