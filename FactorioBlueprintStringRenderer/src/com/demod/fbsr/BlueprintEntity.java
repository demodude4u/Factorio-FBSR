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

	private final boolean jsonNewFormat;

	public BlueprintEntity(JSONObject entityJson, MapVersion version) {
		json = entityJson;

		id = entityJson.getInt("entity_number");
		name = entityJson.getString("name");

		position = Utils.parsePoint2D(entityJson.getJSONObject("position"));

		direction = Direction.fromEntityJSON(entityJson, version);

		jsonNewFormat = version.greaterOrEquals(Blueprint.VERSION_NEW_FORMAT);
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

	public boolean isJsonNewFormat() {
		return jsonNewFormat;
	}

	public JSONObject json() {
		return json;
	}

}
