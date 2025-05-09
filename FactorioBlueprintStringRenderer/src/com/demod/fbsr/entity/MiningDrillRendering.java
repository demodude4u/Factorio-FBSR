package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public class MiningDrillRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	private Optional<FPSprite4Way> protoBasePicture;
	private Optional<FPWorkingVisualisations> protoGraphicsSet;

	// pumpjack:
	// - base_picture
	// - graphics_set.animation

	// electric-mining-drill:
	// - graphics_set.animation
	// - graphics_set.working_visualisations.N/S/E/W

	// burner-mining-drill:
	// - graphics_set.animation

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);

		protoBasePicture.ifPresent(fp -> fp.defineSprites(entityRegister, entity.getDirection()));
		protoGraphicsSet.ifPresent(fp -> fp.defineSprites(entityRegister, entity.getDirection(), FRAME));
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoBasePicture.ifPresent(fp -> fp.getDefs(register));
		protoGraphicsSet.ifPresent(fp -> fp.getDefs(register, FRAME));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoBasePicture = FPUtils.opt(prototype.lua().get("base_picture"), FPSprite4Way::new);
		protoGraphicsSet = FPUtils.opt(prototype.lua().get("graphics_set"), FPWorkingVisualisations::new);
	}
}
