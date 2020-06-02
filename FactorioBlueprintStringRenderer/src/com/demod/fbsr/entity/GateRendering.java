package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class GateRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		boolean vertical = isVerticalGate(entity);

		String orientation = vertical ? "vertical" : "horizontal";

		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get(orientation + "_animation"));
		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
	}

	private boolean isVerticalGate(BlueprintEntity entity) {
		return entity.getDirection().cardinal() % 2 == 0;
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		if (isVerticalGate(entity)) {
			map.setVerticalGate(entity.getPosition());
		} else {
			map.setHorizontalGate(entity.getPosition());
		}
	}
}
