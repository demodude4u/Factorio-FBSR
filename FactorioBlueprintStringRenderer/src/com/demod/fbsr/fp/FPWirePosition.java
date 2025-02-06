package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPWirePosition {
	public final Optional<FPVector> copper;
	public final Optional<FPVector> red;
	public final Optional<FPVector> green;

	public FPWirePosition(LuaValue lua) {
		copper = FPUtils.opt(lua.get("copper"), FPVector::new);
		red = FPUtils.opt(lua.get("red"), FPVector::new);
		green = FPUtils.opt(lua.get("green"), FPVector::new);
	}
}
