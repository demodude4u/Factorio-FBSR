package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSSignalID {
	public final String type;
	public final String name;
	public final Optional<String> quality;

	public BSSignalID(JSONObject json) {
		type = json.optString("type", "item");
		name = json.getString("name");
		quality = BSUtils.optString(json, "quality");
	}

	public BSSignalID(String legacyId) {
		type = "item";
		name = legacyId;
		quality = Optional.empty();
	}
}
