package com.demod.fbsr.render;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.BlueprintEntity.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;

public class SplitterRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		int[] beltSpriteMapping = TransportBeltRendering.transportBeltSpriteMapping[entity.getDirection()
				.cardinal()][1];
		Sprite belt1Sprite = getSpriteFromAnimation(prototype.lua().get("belt_horizontal"));
		belt1Sprite.source.y = belt1Sprite.source.height * beltSpriteMapping[0];
		Sprite belt2Sprite = new Sprite(belt1Sprite);

		Point2D.Double beltShift = entity.getDirection().left().offset(new Point2D.Double(), 0.5);

		belt1Sprite.bounds.x += beltShift.x;
		belt1Sprite.bounds.y += beltShift.y;
		belt2Sprite.bounds.x -= beltShift.x;
		belt2Sprite.bounds.y -= beltShift.y;

		Sprite sprite = getSpriteFromAnimation(
				prototype.lua().get("structure").get(entity.getDirection().toString().toLowerCase()));

		register.accept(spriteRenderer(Layer.ENTITY, belt1Sprite, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY, belt2Sprite, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY2, sprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		Direction direction = entity.getDirection();
		Point2D.Double belt1Pos = direction.left().offset(entity.getPosition(), 0.5);
		Point2D.Double belt2Pos = direction.right().offset(entity.getPosition(), 0.5);
		map.setBelt(belt1Pos, direction);
		map.setBelt(belt2Pos, direction);
	}
}
