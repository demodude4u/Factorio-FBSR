package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class FluidTurretRendering extends TurretRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.fluidBox(lua.get("fluid_box"));
	}
}
