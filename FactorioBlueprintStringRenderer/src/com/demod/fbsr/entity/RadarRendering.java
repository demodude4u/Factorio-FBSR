package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class RadarRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedSprite(lua.get("pictures"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
