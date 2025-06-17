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
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSAsteroidCollectorEntity;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class AsteroidCollectorRendering extends EntityWithOwnerRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSAsteroidCollectorEntity bsEntity = entity.fromBlueprint();

		MapPosition pos = entity.getPosition();

		if (!bsEntity.chunkFilter.isEmpty()) {

			IconManager iconManager = profile.getIconManager();

			List<IconDef> icons = bsEntity.chunkFilter.stream()
					.flatMap(name -> iconManager.lookupAsteroidChunk(name).stream())
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
				IconDef icon = icons.get(i);
				MapPosition iconPos = iconStartPos.addUnit((i % 2) * iconShift, (i / 2) * iconShift);
				register.accept(new MapIcon(iconPos, icon, iconSize, OptionalDouble.of(iconBorder), false, Optional.empty()));
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSAsteroidCollectorEntity.class;
	}
}
