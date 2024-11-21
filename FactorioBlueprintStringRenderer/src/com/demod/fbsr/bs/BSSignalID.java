package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.ItemQuality;

public class BSSignalID {
	public final String type;
	public final String name;
	public final Optional<ItemQuality> quality;

	public BSSignalID(JSONObject json) {
		type = json.optString("type", "item");
		name = json.getString("name");
		quality = BSUtils.optQuality(json, "quality");
	}
}
