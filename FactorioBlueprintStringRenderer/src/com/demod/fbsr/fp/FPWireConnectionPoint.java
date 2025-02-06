package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPWireConnectionPoint {
	public final FPWirePosition wire;
	public final FPWirePosition shadow;

	public FPWireConnectionPoint(LuaValue lua) {
		wire = new FPWirePosition(lua.get("wire"));
		shadow = new FPWirePosition(lua.get("shadow"));
	}
}
