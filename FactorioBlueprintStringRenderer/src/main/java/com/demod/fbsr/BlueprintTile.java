package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class BlueprintTile {
	private final JSONObject json;

	private final String name;
	private final Double position;

	public BlueprintTile(JSONObject entityJson) {
		json = entityJson;

		name = entityJson.getString("name");

		JSONObject positionJson = entityJson.getJSONObject("position");
		position = new Point2D.Double(positionJson.getDouble("x"), positionJson.getDouble("y"));
	}

	public void debugPrint() {
		System.out.println();
		System.out.println(getName());
		Utils.debugPrintJson(json);
	}

	public String getName() {
		return name;
	}

	public Double getPosition() {
		return position;
	}

	public JSONObject json() {
		return json;
	}

}
