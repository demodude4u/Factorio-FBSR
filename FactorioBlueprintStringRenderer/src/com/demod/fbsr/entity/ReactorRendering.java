package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;

@EntityType("reactor")
public class ReactorRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		LuaValue luaLowerLayerPicture = lua.get("lower_layer_picture");
		if (!luaLowerLayerPicture.isnil()) {
			bind.sprite(luaLowerLayerPicture);
		}
		bind.sprite(lua.get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		for (Direction dir : Direction.values()) {
			map.setHeatPipe(dir.offset(entity.getPosition(), 2));
		}
	}
}
