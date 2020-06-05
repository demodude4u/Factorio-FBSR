package com.demod.fbsr.entity;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.WorldMap;

public class HeatInterfaceRendering extends EntityRendererFactory {
	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setHeatPipe(entity.getPosition());
	}
}
