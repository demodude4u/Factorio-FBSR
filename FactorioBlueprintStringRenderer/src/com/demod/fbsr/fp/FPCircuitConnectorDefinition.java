package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPCircuitConnectorDefinition {
	public final Optional<FPCircuitConnectorSprites> sprites;
	public final Optional<FPWireConnectionPoint> points;

	public FPCircuitConnectorDefinition(LuaValue lua) {
		sprites = FPUtils.opt(lua.get("sprites"), FPCircuitConnectorSprites::new);
		points = FPUtils.opt(lua.get("points"), FPWireConnectionPoint::new);
	}
}
