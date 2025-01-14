package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;

public class FPPipeConnectionDefinition {
	public final Optional<String> flowDirection;
	public final Optional<Direction> direction;
	public final Optional<FPVector> position;

	public FPPipeConnectionDefinition(LuaValue lua) {
		flowDirection = FPUtils.optString(lua.get("flow_direction"));
		direction = FPUtils.optDirection(lua.get("direction"));
		position = FPUtils.opt(lua.get("position"), FPVector::new);
	}
}
