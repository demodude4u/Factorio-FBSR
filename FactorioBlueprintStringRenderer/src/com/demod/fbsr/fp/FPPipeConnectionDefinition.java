package com.demod.fbsr.fp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.google.common.collect.ImmutableList;

public class FPPipeConnectionDefinition {
	public final String flowDirection;
	public final String connectionType;
	public final Optional<Direction> direction;
	public final Optional<FPVector> position;
	public final Optional<List<FPVector>> positions;
	public final List<String> enableWorkingVisualisations;
	public final List<String> connectionCategory;
	
	private final long layerBits;
	private final boolean input;
	private final boolean output;

	private static final Map<String, Integer> categoryIndex = new HashMap<>();
	private static volatile int nextCategoryIndex = 0;
	private static long getCategoryBits(List<String> categories) {
		long bits = 0;
		for (String category : categories) {
			Integer index = categoryIndex.get(category);
			if (index == null) {
				synchronized (categoryIndex) {
					if (nextCategoryIndex >= 64) {
						System.err.println("Warning: Too many pipe connection categories (max 64)");
						System.exit(-1);
					}
					index = categoryIndex.get(category);
					if (index == null) {
						index = nextCategoryIndex++;
						categoryIndex.put(category, index);
					}
				}
			}
			bits |= (1 << index);
		}
		return bits;
	}

	public FPPipeConnectionDefinition(LuaValue lua) {
		flowDirection = lua.get("flow_direction").optjstring("input-output");
		connectionType = lua.get("connection_type").optjstring("normal");
		direction = FPUtils.optDirection(lua.get("direction"));
		position = FPUtils.opt(lua.get("position"), FPVector::new);
		positions = FPUtils.optList(lua.get("positions"), FPVector::new);
		enableWorkingVisualisations = FPUtils.list(lua.get("enable_working_visualisations"), l -> l.tojstring());
		
		LuaValue luaConnectionCategories = lua.get("connection_category");
		if (luaConnectionCategories.isnil()) {
			connectionCategory = ImmutableList.of("default");
		} else if (luaConnectionCategories.isarray()) {
			connectionCategory = FPUtils.list(luaConnectionCategories, l -> l.tojstring());
		} else {
			connectionCategory = ImmutableList.of(luaConnectionCategories.tojstring());
		}
		layerBits = getCategoryBits(connectionCategory);

		input = flowDirection.equals("input") || flowDirection.equals("input-output");
		output = flowDirection.equals("output") || flowDirection.equals("input-output");
	}

	public boolean isInput() {
		return input;
	}

	public boolean isOutput() {
		return output;
	}

	public long getLayerBits() {
		return layerBits;
	}
}
