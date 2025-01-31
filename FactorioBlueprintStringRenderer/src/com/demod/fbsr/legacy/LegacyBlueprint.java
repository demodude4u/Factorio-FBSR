package com.demod.fbsr.legacy;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class LegacyBlueprint {

	public final List<LegacyBlueprintEntity> entities;
	public final List<LegacyBlueprintTile> tiles;
	public final Optional<String> label;

	private final JSONObject json;

	public LegacyBlueprint(JSONObject json) throws IllegalArgumentException {

		entities = BSUtils.list(json, "entities", LegacyBlueprintEntity::new);
		tiles = BSUtils.list(json, "tiles", LegacyBlueprintTile::new);
		label = BSUtils.optString(json, "label");

		this.json = json;
	}

	public JSONObject getJson() {
		return json;
	}

}