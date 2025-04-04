package com.demod.fbsr.bs.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSInserterEntity extends BSEntity {
	public final Optional<BSPosition> pickupPosition;
	public final Optional<BSPosition> dropPosition;
	public final boolean useFilters;
	public final List<BSFilter> filters;
	public final Optional<String> filterMode;

	public BSInserterEntity(JSONObject json) {
		super(json);

		useFilters = json.optBoolean("use_filters");// only sometimes checked
		pickupPosition = BSUtils.optPosition(json, "pickup_position");
		dropPosition = BSUtils.optPosition(json, "drop_position");
		filters = BSUtils.list(json, "filters", BSFilter::new);

		Optional<String> filterMode = BSUtils.optString(json, "filter_mode");
		if (useFilters && filters.isEmpty() && filterMode.isEmpty()) {
			filterMode = Optional.of("blacklist");
		}
		this.filterMode = filterMode;
	}

	public BSInserterEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		pickupPosition = BSUtils.opt(legacy.json(), "pickup_position", j -> {
			Point2D.Double pos = Utils.parsePoint2D(j);
			return new BSPosition(pos.x, pos.y);
		});
		dropPosition = BSUtils.opt(legacy.json(), "drop_position", j -> {
			Point2D.Double pos = Utils.parsePoint2D(j);
			return new BSPosition(pos.x, pos.y);
		});

		filters = BSUtils.list(legacy.json(), "filters", j -> new BSFilter(j.getString("name")));
		useFilters = filters.size() > 0;
		filterMode = BSUtils.optString(legacy.json(), "filter_mode");
	}
}