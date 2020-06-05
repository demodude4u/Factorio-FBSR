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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlueprintEntity other = (BlueprintEntity) obj;
		if (id != other.id)
			return false;
		return true;
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

	@Override
	public int hashCode() {
		return id;
	}

	public JSONObject json() {
		return json;
	}

}
