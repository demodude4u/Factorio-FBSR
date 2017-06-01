package com.demod.fbsr.render;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class GeneratorRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> sprites = getSpritesFromAnimation(prototype.lua()
				.get((entity.getDirection().cardinal() % 2) == 0 ? "vertical_animation" : "horizontal_animation"));
		register.accept(spriteRenderer(sprites, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Direction dir = entity.getDirection();
		Point2D.Double position = entity.getPosition();
		map.setPipe(dir.offset(position, 2), dir);
		map.setPipe(dir.back().offset(position, 2), dir.back());
	}
}
