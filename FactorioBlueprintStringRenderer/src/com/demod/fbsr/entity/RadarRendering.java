package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class RadarRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedSprite(lua.get("pictures"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
