package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPWorkingVisualisations;

public class MiningDrillRendering extends SimpleEntityRendering<BSEntity> {

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
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		super.createRenderers(register, map, entity);

		if (protoBasePicture.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(protoBasePicture.get().createSprites(data, entity.direction),
					entity, drawBounds));
		}

		if (protoGraphicsSet.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(protoGraphicsSet.get().createSprites(data, entity.direction, 0),
					entity, drawBounds));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoBasePicture = FPUtils.opt(prototype.lua().get("base_picture"), FPSprite4Way::new);
		protoGraphicsSet = FPUtils.opt(prototype.lua().get("graphics_set"), FPWorkingVisualisations::new);
	}
}
