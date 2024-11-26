package com.demod.fbsr;

import java.util.LinkedHashMap;
import java.util.Map;

// TODO switch over to the actual layer names and use them from the lua
public enum Layer {
	ZERO("zero"), //
	BACKGROUND_TRANSITIONS("background-transitions"), //
	//
	UNDER_TILES("under-tiles"), //
	DECALS("decals"), //
	ABOVE_TILES("above-tiles"), //
	//
	GROUND_LAYER_1("ground-layer-1"), //
	GROUND_LAYER_2("ground-layer-2"), //
	GROUND_LAYER_3("ground-layer-3"), //
	GROUND_LAYER_4("ground-layer-4"), //
	GROUND_LAYER_5("ground-layer-5"), //
	//
	LOWER_RADIUS_VISUALIZATION, //
	RADIUS_VISUALIZATION, //
	//
	TRANSPORT_BELT_INTEGRATION, //

	//
	RAIL_STONE_BACKGROUND, RAIL_STONE, LOGISTICS_RAIL_IO, RAIL_TIES, RAIL_BACKPLATES, RAIL_METALS, //
	SHADOW_BUFFER, //
	ENTITY, LOGISTICS_MOVE, ENTITY2, ENTITY3, //
	OVERLAY, OVERLAY2, OVERLAY3, OVERLAY4, //
	LOGISTICS_WARP, //
	WIRE, //
	ELEVATED_RAIL_STONE_BACKGROUND, ELEVATED_RAIL_STONE, ELEVATED_LOGISTICS_RAIL_IO, ELEVATED_RAIL_TIES,
	ELEVATED_RAIL_BACKPLATES, ELEVATED_RAIL_METALS, ELEVATED_RAIL_ENTITY, //
	DEBUG, DEBUG_RAIL1, DEBUG_RAIL2, DEBUG_LA1, DEBUG_LA2, DEBUG_P //
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

	private Layer(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}