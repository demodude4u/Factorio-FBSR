package com.demod.fbsr.entity;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSLogisticContainerEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

public class LogisticContainerRendering extends ContainerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("animation"));
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		MapPosition pos = entity.getPosition();
		BSLogisticContainerEntity bsEntity = entity.<BSLogisticContainerEntity>fromBlueprint();

		if (bsEntity.requestFilters.isPresent()) {

			Set<String> outputs = bsEntity.requestFilters.get().sections.stream().flatMap(bs -> bs.filters.stream())
					.map(bs -> bs.name).collect(Collectors.toCollection(LinkedHashSet::new));

			map.getOrCreateLogisticGridCell(Direction.NORTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.NORTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSLogisticContainerEntity.class;
	}
}
