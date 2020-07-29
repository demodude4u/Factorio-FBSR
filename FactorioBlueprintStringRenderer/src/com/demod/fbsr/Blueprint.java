package com.demod.fbsr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.factorio.Utils;

public class Blueprint {

	private final JSONObject json;

	private final List<BlueprintEntity> entities = new ArrayList<>();
	private final List<BlueprintTile> tiles = new ArrayList<>();
	private Optional<String> label;
	private MapVersion version;
	private Optional<JSONArray> icons;

	public Blueprint(JSONObject json) throws IllegalArgumentException {
		this.json = json;

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

		if (blueprintJson.has("version")) {
			version = new MapVersion(blueprintJson.getLong("version"));
		} else {
			version = new MapVersion();
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

	public MapVersion getVersion() {
		return version;
	}

	public JSONObject json() {
		return json;
	}

	public void setIcons(Optional<JSONArray> icons) {
		this.icons = icons;
	}

	public void setLabel(Optional<String> label) {
		this.label = label;
	}

}
