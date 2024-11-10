package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class RailSignalRendering extends EntityRendererFactory {

	private SpriteDef protoRailPiece;
	private List<SpriteDef> protoSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		Sprite railSprite = protoRailPiece.createSprite();
		railSprite.source.x += railSprite.source.width * (entity.getDirection().ordinal());

		List<Sprite> sprites = protoSprites.stream().map(SpriteDef::createSprite).collect(Collectors.toList());
		for (Sprite sprite : sprites) {
			sprite.source.y += sprite.source.height * (entity.getDirection().ordinal());
		}

		register.accept(RenderUtils.spriteRenderer(railSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(sprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoRailPiece = RenderUtils.getSpriteFromAnimation(prototype.lua().get("rail_piece")).get();
		protoSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("animation"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		map.getOrCreateRailNode(dir.right().offset(pos, dir.isCardinal() ? 1.5 : 1.0)).setSignal(dir.back());
	}
}
