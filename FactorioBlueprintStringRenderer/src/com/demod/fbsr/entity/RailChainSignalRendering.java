package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import com.demod.factorio.DataTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;

public class RailChainSignalRendering extends RailSignalBaseRendering {
	// TODO elevated versions

//	@Override
//	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
//			BSEntity entity) {
//		// TODO figure out wtf is going on with rail sprites... animation? indices?
//		Sprite railSprite = protoRailPiece.createSprite();
//		railSprite.source.x += railSprite.source.width * (entity.direction.ordinal());
//
//		List<Sprite> sprites = protoSprites.stream().map(SpriteDef::createSprite).collect(Collectors.toList());
//		for (Sprite sprite : sprites) {
//			sprite.source.y += sprite.source.height * (entity.direction.ordinal());
//			sprite.source.x += sprite.source.width * (3);
//		}
//
//		// XXX This is a straight up hack
//		Direction shiftDir = entity.direction.right();
//		Point2D.Double shift = new Point2D.Double(shiftDir.getDx(), shiftDir.getDy());
//		if (entity.direction == Direction.WEST || entity.direction == Direction.SOUTH) {
//			shift.x *= 2;
//			shift.y *= 2;
//		}
//		railSprite.bounds.x += shift.x;
//		railSprite.bounds.y += shift.y;
//		RenderUtils.shiftSprites(sprites, shift);
//
//		register.accept(RenderUtils.spriteRenderer(railSprite, entity, protoSelectionBox));
//		register.accept(RenderUtils.spriteRenderer(sprites, entity, protoSelectionBox));
//	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSRailSignalBaseEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		map.getOrCreateRailNode(dir.right().offset(pos, dir.isCardinal() ? 1.5 : 1.0)).setSignal(dir.back());
	}
}
