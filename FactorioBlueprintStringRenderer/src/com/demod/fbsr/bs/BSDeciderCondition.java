package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSDeciderCondition extends BSCircuitCondition {
	public final Optional<String> compareType;

	public BSDeciderCondition(JSONObject json) {
		super(json);
		compareType = BSUtils.optString(json, "compare_type");
	}
}
