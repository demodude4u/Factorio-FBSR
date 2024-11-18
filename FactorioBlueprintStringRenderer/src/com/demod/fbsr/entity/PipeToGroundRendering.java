package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class PipeToGroundRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite4Way(lua.get("pictures"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setPipe(entity.getPosition(), entity.getDirection());
	}
}
