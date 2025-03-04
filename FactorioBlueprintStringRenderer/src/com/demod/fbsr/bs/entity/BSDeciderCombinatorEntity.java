package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSDeciderCombinatorEntity extends BSEntity {
	public final Optional<String> playerDescription;
	public final Optional<BSDeciderConditions> deciderConditions;

	public BSDeciderCombinatorEntity(JSONObject json) {
		super(json);

		playerDescription = BSUtils.optString(json, "player_description");

		if (json.has("control_behavior")) {
			JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
			deciderConditions = BSUtils.opt(jsonControlBehavior, "decider_conditions", BSDeciderConditions::new);
		} else {
			deciderConditions = Optional.empty();
		}
	}

	public BSDeciderCombinatorEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		playerDescription = Optional.empty();

		String comparatorString = legacy.json().getJSONObject("control_behavior")
				.getJSONObject("decider_conditions").getString("comparator");
		deciderConditions = Optional.of(new BSDeciderConditions(comparatorString));
	}
}