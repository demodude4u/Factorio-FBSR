package com.demod.fbsr.legacy;

import java.awt.geom.Point2D;

import org.json.JSONObject;

import com.demod.factorio.Utils;

public class LegacyBlueprintEntity {
	public final int id;
	public final String name;
	public final Point2D.Double position;
	public final LegacyDirection direction;

	private final JSONObject json;

//	// Artillery Wagon, Rolling Stock
//	public final OptionalDouble orientation;
//
//	// Splitter
//	public final Optional<String> filter;
//	public final Optional<String> inputPriority;
//	public final Optional<String> outputPriority;
//
//	// Loader, Inserter
//	public final List<String> filters;
//
//	// Loader, Underground Belt
//	public final Optional<String> type;
//
//	// Inserter
//	public final Optional<Point2D.Double> pickupPosition;
//	public final Optional<Point2D.Double> dropPosition;
//
//	// Train Stop
//	public final Optional<Color> color;
//	public final Optional<String> station;
//
//	// Assembling Machine
//	public final Optional<String> recipe;

	public LegacyBlueprintEntity(JSONObject json) {

		id = json.getInt("entity_number");
		name = json.getString("name");
		position = Utils.parsePoint2D(json.getJSONObject("position"));
		direction = LegacyDirection.fromEntityJSON(json);

//		orientation = BSUtils.optDouble(json, "orientation");
//		filters = BSUtils.list(json, "filters", j -> j.getString("name"));
//		filter = BSUtils.optString(json, "filter");
//		type = BSUtils.optString(json, "type");
//		inputPriority = BSUtils.optString(json, "input_priority");
//		outputPriority = BSUtils.optString(json, "output_priority");
//		pickupPosition = BSUtils.opt(json, "pickup_position", Utils::parsePoint2D);
//		dropPosition = BSUtils.opt(json, "drop_position", Utils::parsePoint2D);
//		color = BSUtils.opt(json, "color", RenderUtils::parseColor);
//		station = BSUtils.optString(json, "station");
//		recipe = BSUtils.optString(json, "recipe");

		this.json = json;
	}

	public JSONObject json() {
		return json;
	}
}