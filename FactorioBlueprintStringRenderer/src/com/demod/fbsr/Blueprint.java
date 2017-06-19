package com.demod.fbsr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class Blueprint {

	private final List<BlueprintEntity> entities = new ArrayList<>();
	private final List<BlueprintTile> tiles = new ArrayList<>();

	public Blueprint(JSONObject json) throws IllegalArgumentException, IOException {
		JSONObject blueprintJson = json.getJSONObject("blueprint");
		if (blueprintJson.has("entities")) {
			Utils.forEach(blueprintJson.getJSONArray("entities"), (JSONObject j) -> {
				entities.add(new BlueprintEntity(j));
			});
		}
		if (blueprintJson.has("tiles")) {
			Utils.forEach(blueprintJson.getJSONArray("tiles"), (JSONObject j) -> {
				tiles.add(new BlueprintTile(j));
			});
		}
	}

	public List<BlueprintEntity> getEntities() {
		return entities;
	}

	public List<BlueprintTile> getTiles() {
		return tiles;
	}

}
