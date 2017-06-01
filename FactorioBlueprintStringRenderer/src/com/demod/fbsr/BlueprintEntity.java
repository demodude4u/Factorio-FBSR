package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class BlueprintEntity {
	private final JSONObject json;

	private final int id;
	private final String name;
	private final Double position;
	private final Direction direction;

	public BlueprintEntity(JSONObject entityJson) {
		json = entityJson;

		id = entityJson.getInt("entity_number");
		name = entityJson.getString("name");

		JSONObject positionJson = entityJson.getJSONObject("position");
		position = new Point2D.Double(positionJson.getDouble("x"), positionJson.getDouble("y"));

		direction = Direction.values()[entityJson.optInt("direction", 0)];
	}

	public void debugPrint() {
		System.out.println();
		System.out.println(getName());
		Utils.debugPrintJson(json);
	}

	public Direction getDirection() {
		return direction;
	}

	public int getId() {
		return id;
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
