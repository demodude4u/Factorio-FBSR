package com.demod.fbsr.render;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;

public class SplitterRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		int[] beltSpriteMapping = TransportBeltRendering.transportBeltSpriteMapping[entity.getDirection()
				.cardinal()][1];
		Sprite belt1Sprite = getSpriteFromAnimation(prototype.lua().get("belt_horizontal"));
		belt1Sprite.source.y = belt1Sprite.source.height * beltSpriteMapping[0];
		if (beltSpriteMapping[1] == 1) {
			belt1Sprite.source.x += belt1Sprite.source.width;
			belt1Sprite.source.width *= -1;
		}
		if (beltSpriteMapping[2] == 1) {
			belt1Sprite.source.y += belt1Sprite.source.height;
			belt1Sprite.source.height *= -1;
		}
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
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Direction dir = entity.getDirection();
		Double pos = entity.getPosition();
		Point2D.Double leftPos = dir.left().offset(pos, 0.5);
		Point2D.Double rightPos = dir.right().offset(pos, 0.5);

		setLogisticMove(map, leftPos, dir.frontLeft(), dir);
		setLogisticMove(map, leftPos, dir.frontRight(), dir);
		setLogisticMove(map, leftPos, dir.backLeft(), dir);
		setLogisticMove(map, leftPos, dir.backRight(), dir);
		setLogisticMove(map, rightPos, dir.frontLeft(), dir);
		setLogisticMove(map, rightPos, dir.frontRight(), dir);
		setLogisticMove(map, rightPos, dir.backLeft(), dir);
		setLogisticMove(map, rightPos, dir.backRight(), dir);

		setLogisticWarp(map, leftPos, dir.backLeft(), rightPos, dir.frontLeft());
		setLogisticWarp(map, leftPos, dir.backRight(), rightPos, dir.frontRight());
		setLogisticWarp(map, rightPos, dir.backLeft(), leftPos, dir.frontLeft());
		setLogisticWarp(map, rightPos, dir.backRight(), leftPos, dir.frontRight());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Direction direction = entity.getDirection();
		Point2D.Double belt1Pos = direction.left().offset(entity.getPosition(), 0.5);
		Point2D.Double belt2Pos = direction.right().offset(entity.getPosition(), 0.5);
		map.setBelt(belt1Pos, direction, false, true);
		map.setBelt(belt2Pos, direction, false, true);
	}
}
