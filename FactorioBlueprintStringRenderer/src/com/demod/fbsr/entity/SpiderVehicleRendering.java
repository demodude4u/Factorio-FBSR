package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapRenderable;

@EntityType("spider-vehicle")
public class SpiderVehicleRendering extends VehicleRendering {

	// TODO rendering spider hard, just use icon for now

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		ModdingResolver resolver = entity.getResolver();

		Optional<IconDef> icon = resolver.resolveIconEntityName(entity.fromBlueprint().name);
		if (icon.isPresent()) {
			register.accept(
					new MapIcon(entity.getPosition(), icon.get(), 2, OptionalDouble.of(0.2), false, Optional.empty(), resolver));
		}
	}

}
