package com.demod.fbsr.legacy;

import java.awt.geom.Point2D.Double;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class LegacyBlueprintTile {
	public final String name;
	public final Double position;

	private final JSONObject json;

	public LegacyBlueprintTile(JSONObject json) {
		name = json.getString("name");
		position = Utils.parsePoint2D(json.getJSONObject("position"));

		this.json = json;
	}

	public JSONObject getJson() {
		return json;
	}
}