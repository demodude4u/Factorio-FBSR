package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public abstract class ContainerRendering<E extends BSEntity> extends SimpleEntityRendering<E> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		LuaValue luaPicture = lua.get("picture");
		if (!luaPicture.isnil()) {
			bind.sprite(luaPicture);
		}
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
