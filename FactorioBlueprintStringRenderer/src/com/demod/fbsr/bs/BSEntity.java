package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalDouble;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;

public class BSEntity {
	private final JSONObject json;
	private Optional<Exception> parseException = Optional.empty();
	private Object parsedRaw = null;

	// TODO change this to load dynamically the structure that matches the prototype
	// (some things like "filter" are used with different structures)
	// setup functions to cache dynamic parsing

	public final int entityNumber;
	public final String name;
	public final BSPosition position;
	public final Direction direction;
	public final int directionRaw;
	public final OptionalDouble orientation;

//	public final Optional<BSControlBehavior> controlBehavior;
//	public final Optional<ItemQuality> recipeQuality;
//	public final Optional<BSEntityRequestFilters> requestFilters;
//	public final Optional<String> filterMode;
//	public final boolean useFilters;
//	public final OptionalInt overrideStackSize;
//	public final OptionalInt bar;
//	public final List<BSFilter> filters;
//	public final List<BSItemStack> items;
//	public final OptionalInt transitionalRequestIndex;
//	public final Optional<BSSignalID> icon;
//	public final boolean alwaysShow;
//	public final Optional<BSColor> color;
//	public final Optional<String> spoilPriority;
//	public final Optional<String> type;
//	public final OptionalInt manualTrainsLimit;
//	public final OptionalInt priority;
//	public final Optional<String> station;
//	public final Optional<String> text;
//	public final Optional<ItemQuality> quality;
//	public final Optional<BSInfinitySettings> infinitySettings;
//	public final Optional<BSPosition> pickupPosition;
//	public final Optional<BSPosition> dropPosition;
//	public final Optional<String> inputPriority;
//	public final Optional<String> outputPriority;
//	public final Optional<BSFilter> filter;
//	public final Optional<String> railLayer;
//	private final boolean mirror;

	public BSEntity(JSONObject json) {
		entityNumber = json.getInt("entity_number");
		name = json.getString("name");
		position = new BSPosition(json.getJSONObject("position"));
		direction = BSUtils.direction(json, "direction");
		directionRaw = json.optInt("direction");
		orientation = BSUtils.optDouble(json, "orientation");

//		controlBehavior = BSUtils.opt(json, "control_behavior", BSControlBehavior::new);
//		recipeQuality = BSUtils.optQuality(json, "recipe_quality");
//		requestFilters = BSUtils.opt(json, "request_filters", BSEntityRequestFilters::new);
//		filterMode = BSUtils.optString(json, "filter_mode");
//		useFilters = json.optBoolean("use_filters");// only sometimes checked
//		overrideStackSize = BSUtils.optInt(json, "override_stack_size");
//		bar = BSUtils.optInt(json, "bar");
//		filters = BSUtils.list(json, "filters", BSFilter::new);
//		items = BSUtils.list(json, "items", BSItemStack::new);
//		transitionalRequestIndex = BSUtils.optInt(json, "transitional_request_index");
//		icon = BSUtils.opt(json, "icon", BSSignalID::new);
//		alwaysShow = json.optBoolean("always_show");
//		color = BSUtils.opt(json, "color", BSColor::new);
//		spoilPriority = BSUtils.optString(json, "spoil_priority");
//		type = BSUtils.optString(json, "type");
//		manualTrainsLimit = BSUtils.optInt(json, "manual_trains_limit");
//		priority = BSUtils.optInt(json, "priority");
//		station = BSUtils.optString(json, "station");
//		text = BSUtils.optString(json, "text");
//		quality = BSUtils.optQuality(json, "quality");
//		infinitySettings = BSUtils.opt(json, "infinity_settings", BSInfinitySettings::new);
//		pickupPosition = BSUtils.opt(json, "pickup_position", BSPosition::new);
//		dropPosition = BSUtils.opt(json, "drop_position", BSPosition::new);
//		inputPriority = BSUtils.optString(json, "input_priority");
//		outputPriority = BSUtils.optString(json, "output_priority");
//		filter = BSUtils.opt(json, "filter", BSFilter::new);
//		railLayer = BSUtils.optString(json, "rail_layer");
//		// TODO find blueprints that use mirror
//		mirror = json.optBoolean("mirror");

		this.json = json;
	}

	public JSONObject getJson() {
		return json;
	}

	@SuppressWarnings("unchecked")
	public <T> T getParsed() {
		return (T) parsedRaw;
	}

	public Optional<Exception> getParseException() {
		return parseException;
	}

	public void setParsed(Object parsed) {
		this.parsedRaw = parsed;
	}

	public void setParseException(Optional<Exception> parseException) {
		this.parseException = parseException;
	}
}
