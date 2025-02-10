package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class PipeToGroundRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite4Way(lua.get("pictures"));
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		super.populateWorldMap(map, entity);

		map.setPipe(entity.position.createPoint(), entity.direction);
	}
}
