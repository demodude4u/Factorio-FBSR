package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSConstantCombinatorEntity extends BSEntity {
		public final Optional<String> playerDescription;

		public BSConstantCombinatorEntity(JSONObject json) {
			super(json);

			playerDescription = BSUtils.optString(json, "player_description");

			// TODO sections in control_behavior
//			if (json.has("control_behavior")) {
//				JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");

//			} else {

//			}
		}

		public BSConstantCombinatorEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			playerDescription = Optional.empty();
		}
	}