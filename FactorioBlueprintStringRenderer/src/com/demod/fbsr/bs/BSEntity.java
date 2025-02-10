package com.demod.fbsr.bs;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BSEntity {
	public static final Map<String, String> LEGACY_NAME_CONVERT = ImmutableMap.<String, String>builder()//
			.put("curved-rail", "legacy-curved-rail")//
			.put("straight-rail", "legacy-straight-rail")//
			.put("logistic-chest-active-provider", "active-provider-chest")//
			.put("logistic-chest-passive-provider", "passive-provider-chest")//
			.put("logistic-chest-storage", "storage-chest")//
			.put("logistic-chest-buffer", "buffer-chest")//
			.put("logistic-chest-requester", "requester-chest")//
			.put("stack-inserter", "bulk-inserter")//
			.build();

	// TODO change this to load dynamically the structure that matches the prototype
	// (some things like "filter" are used with different structures)
	// setup functions to cache dynamic parsing

	public final int entityNumber;
	public final String name;
	public final BSPosition position;
	public final Direction direction;
	public final int directionRaw;

	public final OptionalDouble orientation;

//	public final Optional<ItemQuality> recipeQuality;
//	public final Optional<String> filterMode;
//	public final OptionalInt overrideStackSize;
//	public final OptionalInt bar;
//	public final OptionalInt transitionalRequestIndex;
//	public final Optional<BSSignalID> icon;
//	public final boolean alwaysShow;
//	public final Optional<String> spoilPriority;
//	public final OptionalInt manualTrainsLimit;
//	public final OptionalInt priority;
//	public final Optional<String> text;
//	public final Optional<ItemQuality> quality;
//	private final boolean mirror;

	public final List<BSItemStack> items;

	public BSEntity(JSONObject json) {
		entityNumber = json.getInt("entity_number");
		name = json.getString("name");
		position = BSUtils.position(json, "position");
		direction = BSUtils.direction(json, "direction");
		directionRaw = json.optInt("direction");
		orientation = BSUtils.optDouble(json, "orientation");

		items = BSUtils.list(json, "items", BSItemStack::new);

//		recipeQuality = BSUtils.optQuality(json, "recipe_quality");
//		requestFilters = BSUtils.opt(json, "request_filters", BSEntityRequestFilters::new);
//		filterMode = BSUtils.optString(json, "filter_mode");
//		overrideStackSize = BSUtils.optInt(json, "override_stack_size");
//		bar = BSUtils.optInt(json, "bar");
//		transitionalRequestIndex = BSUtils.optInt(json, "transitional_request_index");
//		icon = BSUtils.opt(json, "icon", BSSignalID::new);
//		alwaysShow = json.optBoolean("always_show");
//		spoilPriority = BSUtils.optString(json, "spoil_priority");
//		manualTrainsLimit = BSUtils.optInt(json, "manual_trains_limit");
//		priority = BSUtils.optInt(json, "priority");
//		text = BSUtils.optString(json, "text");
//		quality = BSUtils.optQuality(json, "quality");
//		// TODO find blueprints that use mirror
//		mirror = json.optBoolean("mirror");
	}

	public BSEntity(LegacyBlueprintEntity legacy) {

		entityNumber = legacy.id;

		String name = legacy.name;
		name = LEGACY_NAME_CONVERT.getOrDefault(name, name);
		this.name = name;
		Point2D.Double pos = legacy.position;
		position = new BSPosition(pos.x, pos.y);
		direction = legacy.direction.toNewDirection();
		directionRaw = direction.ordinal() * 2;
		orientation = legacy.orientation;

		JSONObject json = legacy.json();

		if (json.has("items")) {
			Object jsonItems = json.get("items");
			if (jsonItems instanceof JSONArray) {
				items = BSUtils.list(json, "items", j -> new BSItemStack(j.getString("item"), j.getInt("count")));
			} else if (jsonItems instanceof JSONObject) {
				items = ((JSONObject) jsonItems).keySet().stream()
						.map(item -> new BSItemStack(item, ((JSONObject) jsonItems).getInt(item)))
						.collect(Collectors.toList());
			} else {
				items = ImmutableList.of();
			}
		} else {
			items = ImmutableList.of();
		}
	}
}
