package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.entity.BSLogisticContainerEntity;

public class LogisticContainerRendering extends ContainerRendering<BSLogisticContainerEntity> {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("animation"));
	}

	@Override
	public void populateLogistics(WorldMap map, BSLogisticContainerEntity entity) {
		Point2D.Double pos = entity.position.createPoint();

		if (entity.requestFilters.isPresent()) {

			Set<String> outputs = entity.requestFilters.get().sections.stream().flatMap(bs -> bs.filters.stream())
					.map(bs -> bs.name).collect(Collectors.toCollection(LinkedHashSet::new));

			map.getOrCreateLogisticGridCell(Direction.NORTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.NORTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
		}
	}
}
