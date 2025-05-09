package com.demod.fbsr.entity;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.bs.entity.BSConstantCombinatorEntity;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class ConstantCombinatorRendering extends EntityWithOwnerRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();
		BSConstantCombinatorEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.controlBehavior.isPresent() && map.isAltMode()) {

			List<BSFilter> filters = bsEntity.controlBehavior.get().sections.stream().flatMap(bs -> bs.filters.stream())
					.collect(Collectors.toList());
			if (!filters.isEmpty()) {

				List<IconDefWithQuality> icons = filters.stream()
						.flatMap(f -> IconManager.lookupFilter(f.type, f.name, f.quality).stream())
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
					register.accept(icon.createMapIcon(iconPos, iconSize, OptionalDouble.of(iconBorder), false));
				}
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.sprite4Way(lua.get("sprites"));
	}

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {
		List<FPWireConnectionPoint> protoConnectionPoints = FPUtils.list(lua.get("circuit_wire_connection_points"),
				FPWireConnectionPoint::new);

		consumer.accept(1, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.RED, true));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.GREEN, true));
	}

	// TODO what am I doing with the custom entity?
	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSConstantCombinatorEntity.class;
	}
}
