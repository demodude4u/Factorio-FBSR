package com.demod.fbsr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.factorio.Utils;

public class Blueprint {

	private final List<BlueprintEntity> entities = new ArrayList<>();
	private final List<BlueprintTile> tiles = new ArrayList<>();
	private final Optional<String> label;
	private final Optional<JSONArray> icons;

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

		if (blueprintJson.has("label")) {
			label = Optional.of(blueprintJson.getString("label"));
		} else {
			label = Optional.empty();
		}

		if (blueprintJson.has("icons")) {
			icons = Optional.of(blueprintJson.getJSONArray("icons"));
		} else {
			icons = Optional.empty();
		}
	}

	public List<BlueprintEntity> getEntities() {
		return entities;
	}

	public Optional<JSONArray> getIcons() {
		return icons;
	}

	public Optional<String> getLabel() {
		return label;
	}

	public List<BlueprintTile> getTiles() {
		return tiles;
	}

}
