package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.ArithmeticCombinatorRendering;
import com.demod.fbsr.entity.ArithmeticCombinatorRendering.BSArithmeticConditions;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSArithmeticCombinatorEntity extends BSEntity {
	public final Optional<String> playerDescription;
	public final Optional<BSArithmeticConditions> arithmeticConditions;

	public BSArithmeticCombinatorEntity(JSONObject json) {
		super(json);

		playerDescription = BSUtils.optString(json, "player_description");

		if (json.has("control_behavior")) {
			JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
			arithmeticConditions = BSUtils.opt(jsonControlBehavior, "arithmetic_conditions",
					BSArithmeticConditions::new);
		} else {
			arithmeticConditions = Optional.empty();
		}
	}

	public BSArithmeticCombinatorEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		playerDescription = Optional.empty();

		String operationString = legacy.json().getJSONObject("control_behavior")
				.getJSONObject("arithmetic_conditions").getString("operation");
		arithmeticConditions = Optional.of(new BSArithmeticConditions(operationString));
	}
}