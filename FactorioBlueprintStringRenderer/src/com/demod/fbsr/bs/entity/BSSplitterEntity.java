package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSSplitterEntity extends BSEntity {
	public final Optional<String> inputPriority;
	public final Optional<String> outputPriority;
	public final Optional<BSFilter> filter;

	public BSSplitterEntity(JSONObject json) {
		super(json);

		inputPriority = BSUtils.optString(json, "input_priority");
		outputPriority = BSUtils.optString(json, "output_priority");
		filter = BSUtils.opt(json, "filter", BSFilter::new);
	}

	public BSSplitterEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		inputPriority = BSUtils.optString(legacy.json(), "input_priority");
		outputPriority = BSUtils.optString(legacy.json(), "output_priority");
		filter = BSUtils.optString(legacy.json(), "filter").map(s -> new BSFilter(s));
	}
}