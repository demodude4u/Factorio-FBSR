package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
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

public class RailChainSignalRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite railSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("rail_piece"));
		railSprite.source.x += railSprite.source.width * (entity.getDirection().ordinal());

		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("animation"));
		for (Sprite sprite : sprites) {
			sprite.source.y += sprite.source.height * (entity.getDirection().ordinal());
			sprite.source.x += sprite.source.width * (3);
		}

		// XXX This is a straight up hack
		Direction shiftDir = entity.getDirection().right();
		Point2D.Double shift = new Point2D.Double(shiftDir.getDx(), shiftDir.getDy());
		if (entity.getDirection() == Direction.WEST || entity.getDirection() == Direction.SOUTH) {
			shift.x *= 2;
			shift.y *= 2;
		}
		railSprite.bounds.x += shift.x;
		railSprite.bounds.y += shift.y;
		RenderUtils.shiftSprites(sprites, shift);

		register.accept(RenderUtils.spriteRenderer(railSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		map.getOrCreateRailNode(dir.right().offset(pos, dir.isCardinal() ? 1.5 : 1.0)).setSignal(dir.back());
	}
}
