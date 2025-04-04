package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSLinkedBeltEntity extends BSEntity {
	public final Optional<String> type;

	public BSLinkedBeltEntity(JSONObject json) {
		super(json);

		type = BSUtils.optString(json, "type");
	}

	public BSLinkedBeltEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		type = BSUtils.optString(legacy.json(), "type");
	}

}