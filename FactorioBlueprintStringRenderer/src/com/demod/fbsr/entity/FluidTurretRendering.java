package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class FluidTurretRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> baseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"),
				entity.getDirection());
		register.accept(RenderUtils.spriteRenderer(baseSprites, entity, prototype));
		List<Sprite> turretSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("folded_animation"),
				entity.getDirection());
		register.accept(RenderUtils.spriteRenderer(turretSprites, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Direction dir = entity.getDirection();
		map.setPipe(dir.right().offset(dir.back().offset(entity.getPosition()), 0.5), dir.right());
		map.setPipe(dir.left().offset(dir.back().offset(entity.getPosition()), 0.5), dir.left());
	}
}
