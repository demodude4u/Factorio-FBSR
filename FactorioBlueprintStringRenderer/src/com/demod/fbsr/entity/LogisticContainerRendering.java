package com.demod.fbsr.entity;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.bs.entity.BSLogisticContainerEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("logistic-container")
public class LogisticContainerRendering extends ContainerRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSLogisticContainerEntity bsEntity = entity.<BSLogisticContainerEntity>fromBlueprint();
		if (!bsEntity.requestFilters.isEmpty()) {
			Optional<BSFilter> filter = bsEntity.requestFilters.get().sections.stream()
					.flatMap(bs -> bs.filters.stream()).filter(f -> f.name.isPresent() || f.quality.isPresent())
					.findAny();
			ModdingResolver resolver = entity.getResolver();
			filter.ifPresent(f -> resolver.resolveFilter(f.type, f.name, f.quality).ifPresent(
					i -> register.accept(i.createMapIcon(entity.getPosition(), 0.5, OptionalDouble.of(0.05), false, resolver))));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("animation"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSLogisticContainerEntity.class;
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);
		
		MapPosition pos = entity.getPosition();
		BSLogisticContainerEntity bsEntity = entity.<BSLogisticContainerEntity>fromBlueprint();

		if (bsEntity.requestFilters.isPresent()) {

			Set<String> outputs = bsEntity.requestFilters.get().sections.stream().flatMap(bs -> bs.filters.stream())
					.flatMap(bs -> bs.name.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

			if (!outputs.isEmpty()) {
				map.getOrCreateLogisticGridCell(Direction.NORTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
				map.getOrCreateLogisticGridCell(Direction.NORTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
				map.getOrCreateLogisticGridCell(Direction.SOUTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
				map.getOrCreateLogisticGridCell(Direction.SOUTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			}
		}
	}
}
