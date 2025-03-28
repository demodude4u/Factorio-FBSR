package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSInfinityFilter {
	public final String mode;
	public final String name;
	public final int count;
	public final int index;
	public final Optional<String> quality;

	public BSInfinityFilter(JSONObject json) {
		mode = json.getString("mode");
		name = json.getString("name");
		count = json.getInt("count");
		index = json.getInt("index");
		quality = BSUtils.optString(json, "quality");
	}

	public BSInfinityFilter(String legacyName) {
		mode = null;
		name = legacyName;
		count = 1;
		index = -1;
		quality = Optional.empty();
	}
}
