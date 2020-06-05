package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
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

public class PumpRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite sprite = RenderUtils.getSpriteFromAnimation(
				prototype.lua().get("animations").get(entity.getDirection().name().toLowerCase()));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		map.setPipe(dir.offset(pos, 0.5), dir);
		map.setPipe(dir.back().offset(pos, 0.5), dir.back());
	}
}
