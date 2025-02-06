package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;

public class FPPipeConnectionDefinition {
	public final Optional<String> flowDirection;
	public final Optional<Direction> direction;
	public final Optional<FPVector> position;

	private final boolean input;
	private final boolean output;

	public FPPipeConnectionDefinition(LuaValue lua) {
		flowDirection = FPUtils.optString(lua.get("flow_direction"));
		direction = FPUtils.optDirection(lua.get("direction"));
		position = FPUtils.opt(lua.get("position"), FPVector::new);

		input = flowDirection.filter(s -> s.equals("input") || s.equals("input-output")).isPresent();
		output = flowDirection.filter(s -> s.equals("output") || s.equals("input-output")).isPresent();
	}

	public boolean isInput() {
		return input;
	}

	public boolean isOutput() {
		return output;
	}
}
