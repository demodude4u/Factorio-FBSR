package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;

public class PipeToGroundRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite4Way(lua.get("pictures"));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		map.setPipe(entity.getPosition(), entity.getDirection());
	}
}
