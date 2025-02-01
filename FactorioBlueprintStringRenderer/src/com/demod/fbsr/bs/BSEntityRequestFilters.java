package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.google.common.collect.ImmutableList;

public class BSEntityRequestFilters {
	public final List<BSSectionFilters> sections;
	public final boolean requestFromBuffers;
	public final boolean trashNotRequested;

	public BSEntityRequestFilters(JSONObject json) {
		sections = BSUtils.list(json, "sections", BSSectionFilters::new);
		requestFromBuffers = json.optBoolean("request_from_buffers");
		trashNotRequested = json.optBoolean("trash_not_requested");
	}

	public BSEntityRequestFilters(List<String> legacyOutputs) {
		if (legacyOutputs.isEmpty()) {
			sections = ImmutableList.of();
		} else {
			sections = ImmutableList.of(new BSSectionFilters(legacyOutputs));
		}
		requestFromBuffers = false;
		trashNotRequested = false;
	}
}
