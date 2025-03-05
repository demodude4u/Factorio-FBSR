package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public class AgriculturalTowerRendering extends SimpleEntityRendering {
	private static final int FRAME = 0;

	private FPWorkingVisualisations protoGraphicsSet;

	// TODO the crane

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Consumer<SpriteDef> spriteRegister = entity.spriteRegister(register, Layer.OBJECT);
		protoGraphicsSet.defineSprites(spriteRegister, entity.getDirection(), FRAME);
	}

	@Override
	public void defineEntity(SimpleEntityRendering.Bindings bind, LuaTable lua) {
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoGraphicsSet.getDefs(register, FRAME);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(prototype.lua().get("graphics_set"));
	}

}
