package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class ThrusterRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.fluidBox(lua.get("fuel_fluid_box"));
		bind.fluidBox(lua.get("oxidizer_fluid_box"));
	}
}
