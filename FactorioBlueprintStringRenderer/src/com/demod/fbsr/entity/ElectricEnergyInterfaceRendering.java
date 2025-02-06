package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.bs.BSEntity;

public class ElectricEnergyInterfaceRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue proto;
		if (!(proto = lua.get("picture")).isnil()) {
			bind.sprite(proto);
		} else if (!(proto = lua.get("pictures")).isnil()) {
			bind.sprite4Way(proto);
		} else if (!(proto = lua.get("animation")).isnil()) {
			bind.animation(proto);
		} else if (!(proto = lua.get("animations")).isnil()) {
			bind.animation4Way(proto);
		}
	}
}
