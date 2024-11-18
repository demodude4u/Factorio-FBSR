package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Renderer.Layer;

public class ElectricPoleRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedSprite(lua.get("pictures")).layer(Layer.ENTITY3);
	}
}
