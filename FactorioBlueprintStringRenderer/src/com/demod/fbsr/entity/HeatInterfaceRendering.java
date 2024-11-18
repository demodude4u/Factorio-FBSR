package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class HeatInterfaceRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("sprite"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setHeatPipe(entity.getPosition());
	}
}
