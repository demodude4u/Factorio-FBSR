package com.demod.fbsr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class Blueprint {

	private final List<BlueprintEntity> entities = new ArrayList<>();

	public Blueprint(JSONObject blueprintJson) throws IllegalArgumentException, IOException {
		Utils.forEach(blueprintJson.getJSONObject("blueprint").getJSONArray("entities"), (JSONObject j) -> {
			entities.add(new BlueprintEntity(j));
		});
	}

	public List<BlueprintEntity> getEntities() {
		return entities;
	}

}
