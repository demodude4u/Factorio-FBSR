package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;

public abstract class ContainerRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaPicture = lua.get("picture");
		if (!luaPicture.isnil()) {
			bind.sprite(luaPicture);
		}
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
