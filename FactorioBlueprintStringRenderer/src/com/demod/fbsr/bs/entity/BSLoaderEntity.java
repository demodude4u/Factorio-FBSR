package com.demod.fbsr.bs.entity;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSLoaderEntity extends BSEntity {
	public final Optional<String> type;
	public final List<BSFilter> filters;

	public BSLoaderEntity(JSONObject json) {
		super(json);

		type = BSUtils.optString(json, "type");
		filters = BSUtils.list(json, "filters", BSFilter::new);
	}

	public BSLoaderEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		type = BSUtils.optString(legacy.json(), "type");
		filters = BSUtils.list(legacy.json(), "filters", j -> new BSFilter(j.getString("name")));
	}

}