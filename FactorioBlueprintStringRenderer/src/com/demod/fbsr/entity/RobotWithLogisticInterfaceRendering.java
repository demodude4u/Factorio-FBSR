package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class RobotWithLogisticInterfaceRendering extends SimpleEntityRendering {
	private static final double ORIENTATION = 0.375;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedAnimation(lua.get("idle")).orientation(ORIENTATION);
	}
}
