package com.demod.fbsr.fp;

import java.util.List;
import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;

public class FPPipeConnectionDefinition {
	public final String flowDirection;
	public final String connectionType;
	public final Optional<Direction> direction;
	public final Optional<FPVector> position;
	public final Optional<List<FPVector>> positions;

	private final boolean input;
	private final boolean output;

	public FPPipeConnectionDefinition(LuaValue lua) {
		flowDirection = lua.get("flow_direction").optjstring("input-output");
		connectionType = lua.get("connection_type").optjstring("normal");
		direction = FPUtils.optDirection(lua.get("direction"));
		position = FPUtils.opt(lua.get("position"), FPVector::new);
		positions = FPUtils.optList(lua.get("positions"), FPVector::new);

		input = flowDirection.equals("input") || flowDirection.equals("input-output");
		output = flowDirection.equals("output") || flowDirection.equals("input-output");
	}

	public boolean isInput() {
		return input;
	}

	public boolean isOutput() {
		return output;
	}
}
