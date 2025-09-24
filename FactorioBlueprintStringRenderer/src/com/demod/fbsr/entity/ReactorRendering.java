package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
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
		bind.heatBuffer(lua.get("heat_buffer"));
		bind.energySource(lua.get("energy_source"));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
	}
}
