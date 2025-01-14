package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
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
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		if (protoBasePicture.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(protoBasePicture.get().createSprites(entity.direction), entity,
					protoSelectionBox));
		}

		if (protoGraphicsSet.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(protoGraphicsSet.get().createSprites(entity.direction, 0),
					entity, protoSelectionBox));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBasePicture = FPUtils.opt(prototype.lua().get("base_picture"), FPSprite4Way::new);
		protoGraphicsSet = FPUtils.opt(prototype.lua().get("graphics_set"), FPWorkingVisualisations::new);
	}
}
