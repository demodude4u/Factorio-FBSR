package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class PowerSwitchRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation(lua.get("power_on_animation"));
	}
}
