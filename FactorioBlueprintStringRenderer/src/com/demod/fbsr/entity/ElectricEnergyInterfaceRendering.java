package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;

public class ElectricEnergyInterfaceRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
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
