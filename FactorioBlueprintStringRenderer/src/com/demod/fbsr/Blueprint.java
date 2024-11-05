package com.demod.fbsr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.factorio.Utils;

public class Blueprint {

	// FIXME Earliest version number I can find with the new format (space age)
	public static final MapVersion VERSION_NEW_FORMAT = new MapVersion(562949954142211L);

	private final JSONObject json;

	private final List<BlueprintEntity> entities = new ArrayList<>();
	private final List<BlueprintTile> tiles = new ArrayList<>();
	private Optional<String> label;
	private MapVersion version;
	private Optional<JSONArray> icons;
	private boolean modsDetected = false;
	private OptionalLong renderTime = OptionalLong.empty();

	public Blueprint(JSONObject json) throws IllegalArgumentException {
		this.json = json;

		JSONObject blueprintJson = json.getJSONObject("blueprint");

		if (blueprintJson.has("version")) {
			version = new MapVersion(blueprintJson.getLong("version"));
		} else {
			version = new MapVersion();
		}

		if (blueprintJson.has("entities")) {
			Utils.forEach(blueprintJson.getJSONArray("entities"), (JSONObject j) -> {
				entities.add(new BlueprintEntity(j, version));
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

	public OptionalLong getRenderTime() {
		return renderTime;
	}

	public List<BlueprintTile> getTiles() {
		return tiles;
	}

	public MapVersion getVersion() {
		return version;
	}

	public boolean isModsDetected() {
		return modsDetected;
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

	public void setModsDetected() {
		modsDetected = true;
	}

	public void setRenderTime(long renderTime) {
		this.renderTime = OptionalLong.of(renderTime);
	}

}
