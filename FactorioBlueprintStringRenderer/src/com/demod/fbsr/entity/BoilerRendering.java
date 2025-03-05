package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPBoilerPictureSet;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class BoilerRendering extends SimpleEntityRendering {
	private static final int FRAME = 0;

	private FPBoilerPictureSet protoPictures;
	private boolean protoHasEnergySource;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Consumer<SpriteDef> spriteRegister = entity.spriteRegister(register, Layer.OBJECT);
		protoPictures.defineSprites(spriteRegister, entity.getDirection(), FRAME);
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.fluidBox(lua.get("fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		for (Direction direction : Direction.cardinals()) {
			protoPictures.defineSprites(register, direction, FRAME);
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoPictures = new FPBoilerPictureSet(prototype.lua().get("pictures"));
		protoHasEnergySource = !prototype.lua().get("energy_source").isnil();
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		Direction dir = entity.getDirection();
		MapPosition position = dir.back().offset(entity.getPosition(), 0.5);
		if (protoHasEnergySource) {
			map.setHeatPipe(position, dir.back());// TODO in the SimpleEntityRendering
		}
	}

}
