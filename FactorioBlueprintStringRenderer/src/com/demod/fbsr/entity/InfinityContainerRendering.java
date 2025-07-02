package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSInfinityContainerEntity;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

import net.dv8tion.jda.api.entities.Icon;

@EntityType("infinity-container")
public class InfinityContainerRendering extends ContainerRendering {
	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSInfinityContainerEntity bsEntity = entity.<BSInfinityContainerEntity>fromBlueprint();

		if (bsEntity.infinitySettings.isPresent() && map.isAltMode()) {
			List<String> items = bsEntity.infinitySettings.get().filters.stream().filter(bs -> bs.count > 0)
					.map(bs -> bs.name).collect(Collectors.toList());

			// TODO show double/quad icons if more than one
			if (!items.isEmpty()) {
				String itemName = items.get(0);

				ModdingResolver resolver = entity.getResolver();

				Optional<IconDef> icon = resolver.resolveIconItemName(itemName);
				if (icon.isPresent()) {
					register.accept(new MapIcon(entity.getPosition(), icon.get(), 0.6, OptionalDouble.of(0.1), false,
							Optional.empty(), resolver));
				}
			}
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSInfinityContainerEntity.class;
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);
		
		MapPosition pos = entity.getPosition();

		BSInfinityContainerEntity bsEntity = entity.<BSInfinityContainerEntity>fromBlueprint();

		if (bsEntity.infinitySettings.isPresent()) {

			Set<String> outputs = bsEntity.infinitySettings.get().filters.stream().filter(bs -> bs.count > 0)
					.map(bs -> bs.name).collect(Collectors.toSet());

			map.getOrCreateLogisticGridCell(Direction.NORTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.NORTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
		}
	}
}
