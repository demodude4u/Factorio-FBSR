package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;

@EntityType("heat-interface")
public class HeatInterfaceRendering extends EntityWithOwnerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.sprite(lua.get("picture"));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		map.setHeatPipe(entity.getPosition());
	}
}
