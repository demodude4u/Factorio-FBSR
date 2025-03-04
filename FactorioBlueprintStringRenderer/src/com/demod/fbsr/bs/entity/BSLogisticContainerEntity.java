package com.demod.fbsr.bs.entity;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSEntityRequestFilters;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSLogisticContainerEntity extends BSEntity {
	public final Optional<BSEntityRequestFilters> requestFilters;

	public BSLogisticContainerEntity(JSONObject json) {
		super(json);

		requestFilters = BSUtils.opt(json, "request_filters", BSEntityRequestFilters::new);
	}

	public BSLogisticContainerEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		List<String> outputs = BSUtils.list(legacy.json(), "request_filters", j -> j.getString("name"));
		if (outputs.isEmpty()) {
			requestFilters = Optional.empty();
		} else {
			requestFilters = Optional.of(new BSEntityRequestFilters(outputs));
		}
	}
}