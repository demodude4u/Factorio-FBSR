package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.WirePoint;
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
	public void createWireConnector(Consumer<MapRenderable> register, BiConsumer<Integer, WirePoint> registerWirePoint,
			MapEntity entity, List<MapEntity> wired, WorldMap map) {
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
