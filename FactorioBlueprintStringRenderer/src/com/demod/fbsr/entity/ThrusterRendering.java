package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class ThrusterRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.fluidBox(lua.get("fuel_fluid_box"));
		bind.fluidBox(lua.get("oxidizer_fluid_box"));
	}
}
