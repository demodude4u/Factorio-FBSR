package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSDeciderCondition extends BSCircuitCondition {
	public final String compareType;

	public BSDeciderCondition(JSONObject json) {
		super(json);
		compareType = json.optString("compare_type", "or");
	}
}