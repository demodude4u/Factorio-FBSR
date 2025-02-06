package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class PumpRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation4Way(lua.get("animations"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("fluid_box"));
	}
}
