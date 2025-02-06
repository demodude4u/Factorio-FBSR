package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class DisplayPanelRendering extends SimpleEntityRendering<BSEntity> {

	// TODO parse entity control_behavior, icon, always_show, text, etc...

	@Override
	public void defineEntity(SimpleEntityRendering<BSEntity>.Bindings bind, LuaTable lua) {
		bind.sprite4Way(lua.get("sprites"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

}
