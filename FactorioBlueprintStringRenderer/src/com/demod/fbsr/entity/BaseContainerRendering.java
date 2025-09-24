package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.bind.Bindings;

public abstract class BaseContainerRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		LuaValue luaPicture = lua.get("picture");
		if (!luaPicture.isnil()) {
			bind.sprite(luaPicture);
		}
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
