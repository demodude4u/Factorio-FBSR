package com.demod.fbsr.render;

import java.awt.geom.Point2D;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.BlueprintEntity.Direction;
import com.demod.fbsr.WorldMap;

public class TransportBeltRendering extends TypeRendererFactory {

	public static final int[][][] transportBeltSpriteMapping = //
			new int[/* NESW */][/* LNR */][/* SXY */] { //
					{ { 8, 1, 0 }, { 1, 0, 0 }, { 8, 0, 0 } }, // North
					{ { 9, 0, 0 }, { 0, 0, 0 }, { 11, 0, 0 } }, // East
					{ { 10, 1, 0 }, { 1, 0, 1 }, { 10, 0, 0 } }, // South
					{ { 11, 1, 0 }, { 0, 1, 0 }, { 9, 1, 0 } }, // West
			};

	public boolean beltFacingMeFrom(UnaryOperator<Direction> rotateFunction, WorldMap map, BlueprintEntity entity) {
		Point2D.Double adjacentPosition = rotateFunction.apply(entity.getDirection()).offset(entity.getPosition());
		Optional<Direction> adjacentDirection = map.getBelt(adjacentPosition);
		return adjacentDirection.map(d -> entity.getPosition().distance(d.offset(adjacentPosition)) < 0.1)
				.orElse(false);
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		boolean left = beltFacingMeFrom(Direction::left, map, entity);
		boolean right = beltFacingMeFrom(Direction::right, map, entity);
		boolean back = beltFacingMeFrom(Direction::back, map, entity);

		int bend;
		if (back || (left && right)) {
			bend = 1; // none
		} else if (left) {
			bend = 0; // from the left
		} else if (right) {
			bend = 2; // from the right
		} else {
			bend = 1; // none
		}
		int[] spriteMapping = transportBeltSpriteMapping[entity.getDirection().cardinal()][bend];

		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("animations"));
		sprite.source.y = sprite.source.height * spriteMapping[0];
		if (spriteMapping[1] == 1) {
			sprite.source.x += sprite.source.width;
			sprite.source.width *= -1;
		}
		if (spriteMapping[2] == 1) {
			sprite.source.y += sprite.source.height;
			sprite.source.height *= -1;
		}

		register.accept(spriteRenderer(sprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		map.setBelt(entity.getPosition(), entity.getDirection());
	}

}
