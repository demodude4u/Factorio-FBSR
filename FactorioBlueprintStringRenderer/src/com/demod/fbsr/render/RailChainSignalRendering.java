package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class RailChainSignalRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite railSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("rail_piece"));
		railSprite.source.x += railSprite.source.width * (entity.getDirection().ordinal());

		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("animation"));
		sprite.source.y += sprite.source.height * (entity.getDirection().ordinal());
		sprite.source.x += sprite.source.width * (3);

		// XXX This is a straight up hack
		Direction shiftDir = entity.getDirection().right();
		double shiftX = shiftDir.getDx();
		double shiftY = shiftDir.getDy();
		if (entity.getDirection() == Direction.WEST || entity.getDirection() == Direction.SOUTH) {
			shiftX *= 2;
			shiftY *= 2;
		}
		railSprite.bounds.x += shiftX;
		railSprite.bounds.y += shiftY;
		sprite.bounds.x += shiftX;
		sprite.bounds.y += shiftY;

		register.accept(RenderUtils.spriteRenderer(railSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
	}
}
