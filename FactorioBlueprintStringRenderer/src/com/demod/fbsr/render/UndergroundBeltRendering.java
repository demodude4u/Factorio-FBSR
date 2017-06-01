package com.demod.fbsr.render;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;

public class UndergroundBeltRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		// LuaUtils.debugPrintTable("", prototype.lua());
		// System.exit(1);

		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		int[] beltSpriteMapping = TransportBeltRendering.transportBeltSpriteMapping[entity.getDirection()
				.cardinal()][1];
		Sprite beltSprite = getSpriteFromAnimation(prototype.lua().get("belt_horizontal"));
		beltSprite.source.y = beltSprite.source.height * beltSpriteMapping[0];
		if (beltSpriteMapping[1] == 1) {
			beltSprite.source.x += beltSprite.source.width;
			beltSprite.source.width *= -1;
		}
		if (beltSpriteMapping[2] == 1) {
			beltSprite.source.y += beltSprite.source.height;
			beltSprite.source.height *= -1;
		}
		switch (structDir) {
		case NORTH:
			beltSprite.source.height /= 2;
			beltSprite.source.y += beltSprite.source.height;
			beltSprite.bounds.height /= 2;
			beltSprite.bounds.y += beltSprite.bounds.height;
			break;
		case WEST:
			beltSprite.source.width /= 2;
			beltSprite.source.x += beltSprite.source.width;
			beltSprite.bounds.width /= 2;
			beltSprite.bounds.x += beltSprite.bounds.width;
			break;
		case EAST:
			beltSprite.source.width /= 2;
			beltSprite.bounds.width /= 2;
			break;
		default:
			break;
		}

		Sprite sprite = getSpriteFromAnimation(
				prototype.lua().get("structure").get(input ? "direction_in" : "direction_out").get("sheet"));
		sprite.source.x += sprite.source.width * (structDir.cardinal());

		register.accept(spriteRenderer(Layer.ENTITY, beltSprite, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY2, sprite, entity, prototype));
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Direction dir = entity.getDirection();
		Point2D.Double pos = entity.getPosition();
		boolean input = entity.json().getString("type").equals("input");

		if (input) {
			setLogisticMove(map, pos, dir.backLeft(), dir);
			setLogisticMove(map, pos, dir.backRight(), dir);
			setLogisticAcceptFilter(map, pos, dir.frontLeft(), dir);
			setLogisticAcceptFilter(map, pos, dir.frontRight(), dir);
		} else {
			// XXX really should be a filter that accepts no direction
			setLogisticMoveAndAcceptFilter(map, pos, dir.backLeft(), dir, dir.back());
			setLogisticMoveAndAcceptFilter(map, pos, dir.backRight(), dir, dir.back());
			setLogisticMove(map, pos, dir.frontLeft(), dir);
			setLogisticMove(map, pos, dir.frontRight(), dir);
		}

		if (input) {
			int maxDistance = prototype.lua().get("max_distance").toint();
			for (int offset = 1; offset <= maxDistance; offset++) {
				Point2D.Double targetPos = dir.offset(pos, offset);
				if (map.isMatchingUndergroundBeltEnding(entity.getName(), targetPos, dir)) {
					setLogisticWarp(map, pos, dir.frontLeft(), targetPos, dir.backLeft());
					setLogisticWarp(map, pos, dir.frontRight(), targetPos, dir.backRight());
					break;
				}
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");

		if (input) {
			map.setBelt(entity.getPosition(), entity.getDirection(), false, false);
		} else {
			map.setBelt(entity.getPosition(), entity.getDirection(), false, true);
			map.setUndergroundBeltEnding(entity.getName(), entity.getPosition(), entity.getDirection());
		}
	}
}
