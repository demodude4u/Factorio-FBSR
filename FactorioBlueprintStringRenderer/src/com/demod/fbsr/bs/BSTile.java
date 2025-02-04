package com.demod.fbsr.bs;

import java.awt.geom.Point2D;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.legacy.LegacyBlueprintTile;

public class BSTile {
	public final BSPosition position;
	public final String name;

	public BSTile(JSONObject json) {
		position = BSUtils.position(json, "position");
		name = json.getString("name");
	}

	public BSTile(LegacyBlueprintTile legacy) {
		Point2D.Double pos = legacy.position;
		position = new BSPosition(pos.x, pos.y);
		name = legacy.name;
	}
}
