package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class DisplayPanelRendering extends SimpleEntityRendering<BSEntity> {

	// TODO parse entity control_behavior, icon, always_show, text, etc...

	@Override
	public void defineEntity(SimpleEntityRendering<BSEntity>.Bindings bind, LuaValue lua) {
		bind.sprite4Way(lua.get("sprites"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

}
