package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.fbsr.EntityType;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.control.BSSelectorCombinatorControlBehavior;
import com.demod.fbsr.bs.entity.BSSelectorCombinatorEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("selector-combinator")
public class SelectorCombinatorRendering extends CombinatorRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();
		BSSelectorCombinatorEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.controlBehavior.isPresent() && bsEntity.controlBehavior.get().operation.isPresent()
				&& map.isAltMode()) {

			BSSelectorCombinatorControlBehavior controlBehavior = bsEntity.controlBehavior.get();
			String operation = controlBehavior.operation.get();

			List<IconDefWithQuality> icons = new ArrayList<>();

			IconManager iconManager = profile.getIconManager();

			if (operation.equals("count")) {
				controlBehavior.countSignal.flatMap(s -> iconManager.lookupSignalID(s.type, s.name, s.quality))
						.ifPresent(icons::add);

			} else if (operation.equals("select")) {
				controlBehavior.indexSignal.flatMap(s -> iconManager.lookupSignalID(s.type, s.name, s.quality))
						.ifPresent(icons::add);

			} else if (operation.equals("quality-filter")) {
				controlBehavior.qualityFilter.flatMap(s -> iconManager.lookupFilter(s.type, s.name, s.quality))
						.ifPresent(icons::add);

			} else if (operation.equals("quality-transfer")) {
				controlBehavior.qualitySourceSignal.flatMap(s -> iconManager.lookupSignalID(s.type, s.name, s.quality))
						.ifPresent(icons::add);
				controlBehavior.qualityDestinationSignal
						.flatMap(s -> iconManager.lookupSignalID(s.type, s.name, s.quality)).ifPresent(icons::add);
			}

			if (!icons.isEmpty()) {

				icons = icons.stream().sorted(Comparator.comparing(iwq -> iwq.getDef().getPrototype())).limit(4)
						.collect(Collectors.toList());

				double iconStartY = entity.getDirection().isHorizontal() ? -0.25 : 0;

				MapPosition iconStartPos;
				if (icons.size() == 2) {
					iconStartPos = pos.addUnit(-0.25, iconStartY);
				} else if (icons.size() > 2) {
					iconStartPos = pos.addUnit(-0.25, iconStartY - 0.25);
				} else {
					iconStartPos = pos.addUnit(0, iconStartY);
				}

				boolean iconBig = icons.size() == 1;
				double iconShift = 0.5;
				double iconSize = iconBig ? 0.5 : 0.4;
				double iconBorder = iconBig ? 0.1 : 0.05;

				for (int i = 0; i < icons.size(); i++) {
					IconDefWithQuality icon = icons.get(i);
					MapPosition iconPos = iconStartPos.addUnit((i % 2) * iconShift, (i / 2) * iconShift);
					register.accept(icon.createMapIcon(iconPos, iconSize, OptionalDouble.of(iconBorder), false));
				}
			}
		}
	}

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("select", "min_symbol_sprites");// TODO change to max_symbol_sprites if selectMax true
		operations.put("count", "count_symbol_sprites");
		operations.put("random", "random_symbol_sprites");
		operations.put("stack-size", "stack_size_sprites");
		operations.put("rocket-capacity", "rocket_capacity_sprites");
		operations.put("quality-filter", "quality_symbol_sprites");
		operations.put("quality-transfer", "quality_symbol_sprites");
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSSelectorCombinatorEntity.class;
	}

	@Override
	public Optional<String> getOperation(MapEntity entity) {
		return entity.<BSSelectorCombinatorEntity>fromBlueprint().controlBehavior.flatMap(bs -> bs.operation);
	}
}
