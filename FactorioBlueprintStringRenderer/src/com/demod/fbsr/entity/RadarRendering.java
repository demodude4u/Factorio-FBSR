package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class RadarRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedSprite(lua.get("pictures"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
