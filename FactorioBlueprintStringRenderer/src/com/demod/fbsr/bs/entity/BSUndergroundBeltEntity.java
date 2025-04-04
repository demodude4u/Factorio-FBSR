package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSUndergroundBeltEntity extends BSEntity {
	public final Optional<String> type;

	public BSUndergroundBeltEntity(JSONObject json) {
		super(json);

		type = BSUtils.optString(json, "type");
	}

	public BSUndergroundBeltEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		type = BSUtils.optString(legacy.json(), "type");
	}
}