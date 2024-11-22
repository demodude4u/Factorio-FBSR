package com.demod.fbsr.bs;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.ItemQuality;

public class BSEntity {
	public final int entityNumber;
	public final String name;
	public final BSPosition position;
	public final Direction direction;
	public final int directionRaw;
	public final Optional<BSControlBehavior> controlBehavior;
	public final Optional<String> recipe;
	public final Optional<ItemQuality> recipeQuality;
	public final Optional<BSEntityRequestFilters> requestFilters;
	public final Optional<String> filterMode;
	public final boolean useFilters;
	public final OptionalInt overrideStackSize;
	public final OptionalInt bar;
	public final List<BSFilter> filters;
	public final List<BSItemStack> items;
	public final OptionalInt transitionalRequestIndex;
	public final Optional<BSSignalID> icon;
	public final boolean alwaysShow;
	public final Optional<BSColor> color;
	public final Optional<String> spoilPriority;
	public final Optional<String> type;
	public final OptionalInt manualTrainsLimit;
	public final OptionalInt priority;
	public final Optional<String> station;
	public final Optional<String> text;
	public final Optional<ItemQuality> quality;

	private final JSONObject debugJson;

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

		debugJson = json;
	}

	public JSONObject getDebugJson() {
		return debugJson;
	}
}
