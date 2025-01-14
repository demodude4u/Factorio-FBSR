package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class FluidTurretRendering extends TurretRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		super.defineEntity(bind, lua);

		bind.fluidBox(lua.get("fluid_box"));
	}
}
