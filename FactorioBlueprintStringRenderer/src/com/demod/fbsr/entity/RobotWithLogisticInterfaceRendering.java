package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class RobotWithLogisticInterfaceRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedAnimation(lua.get("idle"));
	}
}
