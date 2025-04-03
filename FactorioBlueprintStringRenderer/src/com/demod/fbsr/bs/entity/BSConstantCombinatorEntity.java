package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.control.BSConstantCombinatorControlBehavior;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSConstantCombinatorEntity extends BSEntity {
	public final Optional<BSConstantCombinatorControlBehavior> controlBehavior;
	public final Optional<String> playerDescription;

	public BSConstantCombinatorEntity(JSONObject json) {
		super(json);

		controlBehavior = BSUtils.opt(json, "control_behavior", BSConstantCombinatorControlBehavior::new);
		playerDescription = BSUtils.optString(json, "player_description");
	}

	public BSConstantCombinatorEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		controlBehavior = Optional.empty();// TODO figure out legacy parsing
		playerDescription = Optional.empty();
	}
}