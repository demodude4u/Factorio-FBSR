package com.demod.fbsr.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class LabRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation(lua.get("off_animation"));
	}

	//TODO needs logistics that represent lab item pass-through, but with inserter limitations
	// @Override
	// public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
	// 	super.createRenderers(register, map, entity);

	// 	MapPosition pos = entity.getPosition();

	// 	List<LogisticGridCell> cells = map.getLogisticGridCells(entity.getBounds().asMapRect());
	// 	List<String> items = cells.stream()
	// 			.flatMap(c -> c.getTransits().stream().flatMap(o -> o.stream())).distinct().collect(Collectors.toList());

	// 	if (items.size() > 0 && map.isAltMode()) {

	// 		List<IconDefWithQuality> icons = items.stream()
	// 				.flatMap(i -> IconManager.lookupItem(i).stream()
	// 						.map(d -> new IconDefWithQuality(d, Optional.empty())))
	// 				.sorted(Comparator.comparing(iwq -> iwq.getDef().getPrototype())).limit(4)
	// 				.collect(Collectors.toList());

	// 		int rowMax = Math.max(2, (icons.size() + 1) / 2);

	// 		double iconGridSize = (icons.size() <= 4) ? 0.8 : 0.5;
	// 		double iconBorder = iconGridSize / 8.0;
	// 		double iconSize = iconGridSize - iconBorder * 2.0;
			
	// 		int rows = (icons.size() + (rowMax - 1)) / rowMax;
	// 		int cols = Math.min(icons.size(), rowMax);
	// 		MapPosition iconStartPos = pos.addUnit(-(cols - 1) / 2.0 * iconGridSize, -(rows - 1) / 2.0 * iconGridSize);

	// 		for (int i = 0; i < icons.size(); i++) {
	// 			IconDefWithQuality icon = icons.get(i);
	// 			MapPosition iconPos = iconStartPos.addUnit((i % cols) * iconGridSize + (i / cols) * (iconGridSize / 2.0), (i / cols) * iconGridSize);
	// 			register.accept(icon.createMapIcon(iconPos, iconSize, OptionalDouble.of(iconBorder), false));
	// 		}
	// 	}
	// }
}
