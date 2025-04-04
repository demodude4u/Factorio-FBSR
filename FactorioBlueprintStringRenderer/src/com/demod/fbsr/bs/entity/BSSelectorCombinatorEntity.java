package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.control.BSSelectorCombinatorControlBehavior;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSSelectorCombinatorEntity extends BSEntity {
	public final Optional<BSSelectorCombinatorControlBehavior> controlBehavior;
	public final Optional<String> playerDescription;

	public BSSelectorCombinatorEntity(JSONObject json) {
		super(json);

		controlBehavior = BSUtils.opt(json, "control_behavior", BSSelectorCombinatorControlBehavior::new);
		playerDescription = BSUtils.optString(json, "player_description");
	}

	public BSSelectorCombinatorEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		// This entity does not exist in legacy
		controlBehavior = Optional.empty();
		playerDescription = Optional.empty();
	}
}