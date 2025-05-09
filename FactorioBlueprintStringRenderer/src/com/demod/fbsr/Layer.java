package com.demod.fbsr;

import java.util.LinkedHashMap;
import java.util.Map;

// Copied from https://lua-api.factorio.com/latest/types/RenderLayer.html
public enum Layer {
	ZERO, //
	BACKGROUND_TRANSITIONS, //
	UNDER_TILES, //
	DECALS, //
	GRID, // Custom
	ABOVE_TILES, //
	GROUND_LAYER_1, //
	GROUND_LAYER_2, //
	GROUND_LAYER_3, //
	GROUND_LAYER_4, //
	GROUND_LAYER_5, //
	LOWER_RADIUS_VISUALIZATION, //
	RADIUS_VISUALIZATION, //
	TRANSPORT_BELT_INTEGRATION, //
	RESOURCE, //
	BUILDING_SMOKE, //
	RAIL_STONE_PATH_LOWER, //
	RAIL_STONE_PATH, //
	LOGISTICS_RAIL_IO, // Custom
	RAIL_TIE, //
	DECORATIVE, //
	GROUND_PATCH, //
	GROUND_PATCH_HIGHER, //
	GROUND_PATCH_HIGHER2, //
	RAIL_SCREW, //
	RAIL_METAL, //
	REMNANTS, //
	FLOOR, //
	TRANSPORT_BELT, //
	TRANSPORT_BELT_ENDINGS, //
	FLOOR_MECHANICS_UNDER_CORPSE, //
	CORPSE, //
	FLOOR_MECHANICS, //
	ITEM, //
	TRANSPORT_BELT_READER, //
	LOWER_OBJECT, //
	TRANSPORT_BELT_CURCUIT_CONNECTOR, //
	GRID_ABOVE_BELTS, // Custom
	SHADOW_BUFFER, // Custom
	LOWER_OBJECT_ABOVE_SHADOW, //
	LOWER_OBJECT_OVERLAY, //
	OBJECT_UNDER, //
	LOGISTICS_MOVE, // Custom
	OBJECT, //
	CARGO_HATCH, //
	HIGHER_OBJECT_UNDER, //
	HIGHER_OBJECT_ABOVE, //
	TRAIN_STOP_TOP, //
	ITEM_IN_INSERTER_HAND, //
	ABOVE_INSERTERS, //
	INSERTER_INDICATORS, // Custom
	WIRE, //
	UNDER_ELEVATED, //
	ELEVATED_RAIL_STONE_PATH_LOWER, //
	ELEVATED_RAIL_STONE_PATH, //
	ELEVATED_LOGISTICS_RAIL_IO, // Custom
	ELEVATED_RAIL_TIE, //
	ELEVATED_RAIL_SCREW, //
	ELEVATED_RAIL_METAL, //
	ELEVATED_LOWER_OBJECT, //
	ELEVATED_RAIL_OBJECT, //
	ELEVATED_HIGHER_OBJECT, //
	FLUID_VISUALIZATION, //
	WIRES_ABOVE, //
	DEBUG, // Custom
	ENTITY_INFO_ICON, //
	ENTITY_INFO_ICON_ABOVE, //
	ENTITY_INFO_TEXT, // Custom
	LOGISTICS_WARP, // Custom
	EXPLOSION, //
	PROJECTILE, //
	SMOKE, //
	AIR_OBJECT, //
	AIR_ENTITY_INFO_ICON, //
	LIGHT_EFFECT, //
	SELECTION_BOX, //
	HIGHER_SELECTION_BOX, //
	COLLISION_SELECTION_BOX, //
	ARROW, //
	BUILDING_PREVIEW_REFERENCE_BOX, //
	CURSOR,//
	;

	private static Map<String, Layer> byKey = new LinkedHashMap<>();
	static {
		for (Layer layer : values()) {
			byKey.put(layer.key, layer);
		}
	}

	public static Layer fromKey(String key) {
		return byKey.get(key);
	}

	private final String key;

	private Layer() {
		this.key = name().toLowerCase().replace('_', '-');
	}

	public String getKey() {
		return key;
	}
}