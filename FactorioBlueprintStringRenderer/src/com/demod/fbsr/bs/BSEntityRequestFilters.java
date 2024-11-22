package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSEntityRequestFilters {
	public final List<BSSectionFilters> sections;
	public final boolean requestFromBuffers;
	public final boolean trashNotRequested;

	public BSEntityRequestFilters(JSONObject json) {
		sections = BSUtils.list(json, "sections", BSSectionFilters::new);
		requestFromBuffers = json.optBoolean("request_from_buffers");
		trashNotRequested = json.optBoolean("trash_not_requested");
	}
}
