package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class LandMineRendering extends SimpleEntityRendering<BSEntity> {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("picture_safe"));
	}
}
