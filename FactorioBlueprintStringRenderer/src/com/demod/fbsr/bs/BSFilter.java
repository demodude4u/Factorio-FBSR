package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSFilter {
	public final int index;
	public final String name;
	public final Optional<String> type;
	public final Optional<String> quality;
	public final Optional<String> comparator;
	public final OptionalInt count;
	public final OptionalInt maxCount;

	public BSFilter(JSONObject json) {
		index = json.optInt("index", 1);
		name = BSUtils.optString(json, "name").orElse("UNKNOWN");// XXX could be handled better
		type = BSUtils.optString(json, "type");// TODO default value item? enum?
		quality = BSUtils.optString(json, "quality");
		comparator = BSUtils.optString(json, "comparator");
		count = BSUtils.optInt(json, "count");
		maxCount = BSUtils.optInt(json, "max_count");
	}

	public BSFilter(String legacyName) {
		index = 1;
		name = legacyName;
		type = Optional.empty();
		quality = Optional.empty();
		comparator = Optional.empty();
		count = OptionalInt.empty();
		maxCount = OptionalInt.empty();
	}
}
