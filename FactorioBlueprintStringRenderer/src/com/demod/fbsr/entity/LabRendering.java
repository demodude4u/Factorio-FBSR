package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class LabRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation(lua.get("off_animation"));
	}
}
