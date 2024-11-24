package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSInfinitySettings {
	public final boolean removeUnfilteredItems;
	public final List<BSInfinityFilter> filters;

	public BSInfinitySettings(JSONObject json) {
		removeUnfilteredItems = json.optBoolean("remove_unfiltered_items");
		filters = BSUtils.list(json, "filters", BSInfinityFilter::new);
	}
}
