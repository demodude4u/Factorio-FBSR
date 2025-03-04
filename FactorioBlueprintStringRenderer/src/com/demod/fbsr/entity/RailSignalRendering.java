package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.entity.BSRailSignalBaseEntity;

public class RailSignalRendering extends RailSignalBaseRendering {

//	private SpriteDef protoRailPiece;
//	private List<SpriteDef> protoSprites;
//
//	@Override
//	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
//			BSEntity entity) {
//		Sprite railSprite = protoRailPiece.createSprite();
//		railSprite.source.x += railSprite.source.width * (entity.direction.ordinal());
//
//		List<Sprite> sprites = protoSprites.stream().map(SpriteDef::createSprite).collect(Collectors.toList());
//		for (Sprite sprite : sprites) {
//			sprite.source.y += sprite.source.height * (entity.direction.ordinal());
//		}
//
//		register.accept(RenderUtils.spriteRenderer(railSprite, entity, drawBounds));
//		register.accept(RenderUtils.spriteRenderer(sprites, entity, drawBounds));
//	}
//
//	@Override
//	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
//		super.initFromPrototype(dataTable, prototype);
//
//		LuaValue groundLua = prototype.lua().get("ground_picture_set");
//		protoRailPiece = RenderUtils.getSpriteFromAnimation(groundLua.get("rail_piece").get("sprites")).get();
//		protoSprites = RenderUtils.getSpritesFromAnimation(groundLua.get("structure"));
//	}

	@Override
	public void populateWorldMap(WorldMap map, BSRailSignalBaseEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		map.getOrCreateRailNode(dir.right().offset(pos, dir.isCardinal() ? 1.5 : 1.0)).setSignal(dir.back());
	}
}
