package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class FusionReactorRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		LuaValue luaGraphicsSet = lua.get("graphics_set");
		bind.sprite4Way(luaGraphicsSet.get("structure"));

	}

	// TODO connectors between adjacent fusion reactors
	// https://factorio.com/blog/post/fff-420

}
