package com.demod.fbsr.entity;

import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

public class RailChainSignalRendering extends RailSignalBaseRendering {
	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();

		map.getOrCreateRailNode(dir.right().offset(pos, dir.isCardinal() ? 1.5 : 1.0)).setSignal(dir.back());
	}
}
