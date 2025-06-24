package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;

@EntityType("thruster")
public class ThrusterRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.fluidBox(lua.get("fuel_fluid_box"));
		bind.fluidBox(lua.get("oxidizer_fluid_box"));
	}
}
