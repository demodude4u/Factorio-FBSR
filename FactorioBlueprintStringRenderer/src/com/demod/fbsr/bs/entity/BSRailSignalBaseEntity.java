package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSRailSignalBaseEntity extends BSEntity {
	public final Optional<String> railLayer;

	public BSRailSignalBaseEntity(JSONObject json) {
		super(json);

		railLayer = BSUtils.optString(json, "rail_layer");
	}

	public BSRailSignalBaseEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		railLayer = Optional.empty();
	}
}