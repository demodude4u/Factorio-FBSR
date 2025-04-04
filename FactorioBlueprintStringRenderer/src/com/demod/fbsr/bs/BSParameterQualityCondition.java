package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSParameterQualityCondition {
	public final String quality;
	public final String comparator;

	public BSParameterQualityCondition(JSONObject json) {
		quality = json.getString("quality");
		comparator = json.getString("comparator");
	}
}
