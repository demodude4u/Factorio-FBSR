package com.demod.fbsr.bs;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;

public class BSEntity {
	private final int entityNumber;
	private final String name;
	private final BSPosition position;
	private final Direction direction;
	private final int directionRaw;

	public BSEntity(JSONObject json) {
		entityNumber = json.getInt("entity_number");
		name = json.getString("name");
		position = new BSPosition(json.getJSONObject("position"));
		direction = BSUtils.direction(json, "direction");
		directionRaw = json.optInt("direction");
		controlBehavior = BSUtils.opt(json, "control_behavior", BSControlBehavior::new);
		recipe = BSUtils.optString(json, "recipe");
		recipeQuality = BSUtils.optQuality(json, "recipe_quality");
		requestFilters = BSUtils.opt(json, "request_filters", BSEntityRequestFilters::new);
		filterMode = BSUtils.optString(json, "filter_mode");
		useFilters = json.optBoolean("use_filters");
		overrideStackSize = BSUtils.optInt(json, "override_stack_size");
		bar = BSUtils.optInt(json, "bar");
		filters = BSUtils.list(json, "filters", BSFilter::new);
		items = BSUtils.list(json, "items", BSItemStack::new);
		transitionalRequestIndex = BSUtils.optInt(json, "transitional_request_index");
		icon = BSUtils.opt(json, "icon", BSSignalID::new);
		alwaysShow = json.optBoolean("always_show");
		color = BSUtils.opt(json, "color", BSColor::new);
		spoilPriority = BSUtils.optString(json, "spoil_priority");
		type = BSUtils.optString(json, "type");
		manualTrainsLimit = BSUtils.optInt(json, "manual_trains_limit");
		priority = BSUtils.optInt(json, "priority");
		station = BSUtils.optString(json, "station");
		text = BSUtils.optString(json, "text");
		quality = BSUtils.optQuality(json, "quality");
	}
}
