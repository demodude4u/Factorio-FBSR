package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;

@EntityType("robot-with-logistic-interface")
public class RobotWithLogisticInterfaceRendering extends EntityWithOwnerRendering {
	private static final double ORIENTATION = 0.375;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.rotatedAnimation(lua.get("idle")).orientation(ORIENTATION);
	}
}
