package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSDeciderCombinatorEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class DeciderCombinatorRendering extends CombinatorRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();
		BSDeciderCombinatorEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.deciderConditions.isPresent()) {

			List<IconDefWithQuality> inputIcons = new ArrayList<>();
			bsEntity.deciderConditions.get().conditions.stream()
					.flatMap(bs -> Arrays.asList(bs.firstSignal, bs.secondSignal).stream().flatMap(s -> s.stream()))
					.limit(2)
					.forEach(s -> IconManager.lookupSignalID(s.type, s.name, s.quality).ifPresent(inputIcons::add));

			List<IconDefWithQuality> outputIcons = new ArrayList<>();
			bsEntity.deciderConditions.get().outputs.stream().flatMap(bs -> bs.signal.stream()).limit(2)
					.forEach(s -> IconManager.lookupSignalID(s.type, s.name, s.quality).ifPresent(outputIcons::add));

			double iconStartY = entity.getDirection().isHorizontal() ? -0.5 : -0.25;

			List<List<IconDefWithQuality>> iconRows = Arrays.asList(inputIcons, outputIcons);
			for (int row = 0; row < iconRows.size(); row++) {
				List<IconDefWithQuality> icons = iconRows.get(row);
				if (!icons.isEmpty()) {
					MapPosition rowPos = pos.addUnit(-(icons.size() - 1) * 0.25, iconStartY + row * 0.5);
					for (int i = 0; i < icons.size(); i++) {
						IconDefWithQuality icon = icons.get(i);
						MapPosition iconPos = rowPos.addUnit(i * 0.5, 0);
						register.accept(icon.createMapIcon(iconPos, 0.4, OptionalDouble.of(0.05), false));
					}
				}
			}
		}
	}

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("=", "equal_symbol_sprites");
		operations.put(">", "greater_symbol_sprites");
		operations.put("<", "less_symbol_sprites");
		operations.put("\u2260", "not_equal_symbol_sprites");
		operations.put("\u2264", "less_or_equal_symbol_sprites");
		operations.put("\u2265", "greater_or_equal_symbol_sprites");

	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSDeciderCombinatorEntity.class;
	}

	@Override
	public Optional<String> getOperation(MapEntity entity) {
		return entity.<BSDeciderCombinatorEntity>fromBlueprint().deciderConditions
				.flatMap(bs -> bs.conditions.stream().findFirst()).map(bs -> bs.comparator);
	}
}
