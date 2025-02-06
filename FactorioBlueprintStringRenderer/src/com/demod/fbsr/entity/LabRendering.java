package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class LabRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation(lua.get("off_animation"));
	}
}
