package com.demod.fbsr.legacy;

import org.json.JSONObject;

import com.demod.fbsr.Direction;

public enum LegacyDirection {
	NORTH, //
	NORTHEAST, //
	EAST, //
	SOUTHEAST, //
	SOUTH, //
	SOUTHWEST, //
	WEST, //
	NORTHWEST;

	public static LegacyDirection fromEntityJSON(JSONObject entityJson) {
		int dir = entityJson.optInt("direction", 0);
		return LegacyDirection.values()[dir];
	}

	public Direction toNewDirection() {
		return Direction.valueOf(name());
	}
}