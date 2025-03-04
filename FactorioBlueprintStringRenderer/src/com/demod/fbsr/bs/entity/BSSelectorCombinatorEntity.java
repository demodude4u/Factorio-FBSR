package com.demod.fbsr.bs.entity;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSSelectorCombinatorEntity extends BSEntity {
	public final Optional<String> playerDescription;
	public final Optional<String> operation;
	public final Optional<Boolean> selectMax;
	public final OptionalInt indexConstant;

	public BSSelectorCombinatorEntity(JSONObject json) {
		super(json);

		playerDescription = BSUtils.optString(json, "player_description");

		if (json.has("control_behavior")) {
			JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
			operation = BSUtils.optString(jsonControlBehavior, "operation");
			selectMax = BSUtils.optBool(jsonControlBehavior, "select_max");
			indexConstant = BSUtils.optInt(jsonControlBehavior, "index_constant");
		} else {
			operation = Optional.empty();
			selectMax = Optional.empty();
			indexConstant = OptionalInt.empty();
		}
	}

	public BSSelectorCombinatorEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		// This entity does not exist in legacy
		playerDescription = Optional.empty();
		operation = Optional.empty();
		selectMax = Optional.empty();
		indexConstant = OptionalInt.empty();
	}
}