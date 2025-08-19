package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapEntityError;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class ErrorRendering extends EntityRendererFactory {

	@Override
	public void createModuleIcons(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		register.accept(new MapEntityError(entity));
	}

	@Override
	public Optional<WirePoint> createWirePoint(Consumer<MapRenderable> register, MapPosition position,
			double orientation, int connectionId) {
		return Optional.empty();
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
	}

	@Override
	public void initFromPrototype() {
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
	}

	@Override
	public boolean isEntityTypeMatch(EntityPrototype proto) {
		return true;
	}

}
