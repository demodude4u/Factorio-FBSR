package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class ReactorRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		LuaValue luaLowerLayerPicture = lua.get("lower_layer_picture");
		if (!luaLowerLayerPicture.isnil()) {
			bind.sprite(luaLowerLayerPicture);
		}
		bind.sprite(lua.get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		for (Direction dir : Direction.values()) {
			map.setHeatPipe(dir.offset(entity.position.createPoint(), 2));
		}
	}
}
