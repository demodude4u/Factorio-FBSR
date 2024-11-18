package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class ElectricEnergyInterfaceRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
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
