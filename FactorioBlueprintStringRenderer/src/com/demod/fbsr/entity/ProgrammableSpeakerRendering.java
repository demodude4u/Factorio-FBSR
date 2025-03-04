package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class ProgrammableSpeakerRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("sprite"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
