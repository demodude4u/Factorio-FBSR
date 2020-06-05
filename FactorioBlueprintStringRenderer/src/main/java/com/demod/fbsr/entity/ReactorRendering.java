package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class ReactorRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		register.accept(RenderUtils.spriteRenderer(RenderUtils.getSpritesFromAnimation(prototype.lua().get("picture"), entity.getDirection()),
				entity, prototype));
		register.accept(RenderUtils.spriteRenderer(
				RenderUtils.getSpritesFromAnimation(prototype.lua().get("lower_layer_picture"), entity.getDirection()), entity,
				prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		for (Direction dir : Direction.values()) {
			map.setHeatPipe(dir.offset(entity.getPosition(), 2));
		}
	}
}
