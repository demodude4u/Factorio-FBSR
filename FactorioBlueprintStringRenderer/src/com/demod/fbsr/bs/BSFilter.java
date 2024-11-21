package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.ItemQuality;

public class BSFilter {
	public final int index;
	public final String name;
	public final Optional<String> type;
	public final Optional<ItemQuality> quality;
	public final Optional<String> comparator;
	public final OptionalInt count;
	public final OptionalInt maxCount;

	public BSFilter(JSONObject json) {
		index = json.getInt("index");
		name = json.getString("name");
		type = BSUtils.optString(json, "type");// TODO default value item? enum?
		quality = BSUtils.optQuality(json, "quality");
		comparator = BSUtils.optString(json, "comparator");
		count = BSUtils.optInt(json, "count");
		maxCount = BSUtils.optInt(json, "max_count");
	}
}