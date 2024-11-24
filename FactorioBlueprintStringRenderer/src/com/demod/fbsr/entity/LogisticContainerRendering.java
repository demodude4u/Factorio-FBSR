package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class LogisticContainerRendering extends ContainerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("animation"));
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSEntity entity) {
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
