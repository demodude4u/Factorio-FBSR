package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class PumpRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("animations"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("fluid_box"));
	}
}
