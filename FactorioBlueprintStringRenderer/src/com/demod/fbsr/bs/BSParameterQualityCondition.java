package com.demod.fbsr.bs;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.ItemQuality;

public class BSParameterQualityCondition {
	public final ItemQuality quality;
	public final String comparator;

	public BSParameterQualityCondition(JSONObject json) {
		quality = BSUtils.quality(json, "quality");
		comparator = json.getString("comparator");
	}
}
