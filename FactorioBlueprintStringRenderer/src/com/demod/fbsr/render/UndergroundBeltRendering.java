package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.BlueprintEntity.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;

public class UndergroundBeltRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
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
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");
		if (!input) {
			map.setBelt(entity.getPosition(), entity.getDirection());
		}
	}
}
