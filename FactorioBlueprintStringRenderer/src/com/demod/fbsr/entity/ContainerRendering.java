package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.bs.BSEntity;

public abstract class ContainerRendering<E extends BSEntity> extends SimpleEntityRendering<E> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaPicture = lua.get("picture");
		if (!luaPicture.isnil()) {
			bind.sprite(luaPicture);
		}
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
