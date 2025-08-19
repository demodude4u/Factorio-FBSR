package com.demod.fbsr.entity;

import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

@EntityType("rail-signal")
public class RailSignalRendering extends RailSignalBaseRendering {
	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		MapPosition pos = entity.getPosition();
		Dir16 dir = Dir16.values()[entity.fromBlueprint().directionRaw];

		// TODO
//		map.getOrCreateRailNode(dir.right().offset(pos, dir.isCardinal() ? 1.5 : 1.0)).setSignal(dir.back());
	}
}
