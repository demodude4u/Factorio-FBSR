package com.demod.fbsr.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.fbsr.EntityType;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("container")
public class ContainerRendering extends BaseContainerRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();

		List<LogisticGridCell> cells = map.getLogisticGridCells(entity.getBounds().asMapRect());
		List<String> items = cells.stream()
				.flatMap(c -> c.getTransits().stream().flatMap(o -> o.stream())).distinct().collect(Collectors.toList());

		if (items.size() > 0 && map.isAltMode()) {

			ModdingResolver resolver = entity.getResolver();

			List<IconDefWithQuality> icons = items.stream()
					.flatMap(i -> resolver.resolveIconItemName(i).stream()
							.map(d -> new IconDefWithQuality(d, Optional.empty())))
					.sorted(Comparator.comparing(iwq -> iwq.getDef().getPrototype())).limit(4)
					.collect(Collectors.toList());

			MapPosition iconStartPos;
			if (icons.size() == 2) {
				iconStartPos = pos.addUnit(-0.25, 0);
			} else if (icons.size() > 2) {
				iconStartPos = pos.addUnit(-0.25, -0.25);
			} else {
				iconStartPos = pos;
			}

			boolean iconBig = icons.size() == 1;
			double iconShift = 0.5;
			double iconSize = iconBig ? 0.5 : 0.4;
			double iconBorder = iconBig ? 0.1 : 0.05;

			for (int i = 0; i < icons.size(); i++) {
				IconDefWithQuality icon = icons.get(i);
				MapPosition iconPos = iconStartPos.addUnit((i % 2) * iconShift, (i / 2) * iconShift);
				register.accept(icon.createMapIcon(iconPos, iconSize, OptionalDouble.of(iconBorder), false, entity.getResolver()));
			}
		}
	}

}
