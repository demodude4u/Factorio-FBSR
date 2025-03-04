package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class ReactorRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaLowerLayerPicture = lua.get("lower_layer_picture");
		if (!luaLowerLayerPicture.isnil()) {
			bind.sprite(luaLowerLayerPicture);
		}
		bind.sprite(lua.get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		super.populateWorldMap(map, entity);

		for (Direction dir : Direction.values()) {
			map.setHeatPipe(dir.offset(entity.position.createPoint(), 2));
		}
	}
}
