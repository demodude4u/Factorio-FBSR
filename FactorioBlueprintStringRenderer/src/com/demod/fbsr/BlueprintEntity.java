package com.demod.fbsr;

import java.awt.geom.Point2D;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class BlueprintEntity {
	private final JSONObject json;

	private final int id;
	private final String name;
	private final Point2D.Double position;
	private final Direction direction;

	public BlueprintEntity(JSONObject entityJson) {
		json = entityJson;

		id = entityJson.getInt("entity_number");
		name = entityJson.getString("name");

		position = Utils.parsePoint2D(entityJson.getJSONObject("position"));

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

	public Point2D.Double getPosition() {
		return position;
	}

	public JSONObject json() {
		return json;
	}

}
