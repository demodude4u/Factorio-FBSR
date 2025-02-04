package com.demod.fbsr.bs;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSInfinitySettings {
	public final boolean removeUnfilteredItems;
	public final List<BSInfinityFilter> filters;

	public BSInfinitySettings(JSONObject json) {
		removeUnfilteredItems = json.optBoolean("remove_unfiltered_items");
		filters = BSUtils.list(json, "filters", BSInfinityFilter::new);
	}

	public BSInfinitySettings(List<String> legacyItems) {
		removeUnfilteredItems = false;
		filters = legacyItems.stream().map(name -> new BSInfinityFilter(name)).collect(Collectors.toList());
	}
}
