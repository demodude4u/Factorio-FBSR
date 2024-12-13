package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class FusionReactorRendering extends SimpleEntityRendering<BSEntity> {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		LuaValue luaGraphicsSet = lua.get("graphics_set");
		bind.sprite4Way(luaGraphicsSet.get("structure"));

	}

	// TODO connectors between adjacent fusion reactors and generators
	// https://factorio.com/blog/post/fff-420

}
