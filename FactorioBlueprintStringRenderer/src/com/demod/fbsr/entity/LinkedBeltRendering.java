package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class LinkedBeltRendering extends EntityRendererFactory {

	private SpriteDef[][] protoBeltSprites;
	private SpriteDef protoInputSprite;
	private SpriteDef protoOutputSprite;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		SpriteDef beltSprite = protoBeltSprites[entity.getDirection().cardinal()][BeltBend.NONE.ordinal()];

		Sprite sprite = (input ? protoInputSprite : protoOutputSprite).createSprite();
		sprite.source.x += sprite.source.width * (structDir.cardinal());

		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY, beltSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, sprite, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBeltSprites = TransportBeltRendering.getBeltSprites(prototype);
		protoInputSprite = RenderUtils
				.getSpriteFromAnimation(prototype.lua().get("structure").get("direction_in").get("sheet")).get();
		protoOutputSprite = RenderUtils
				.getSpriteFromAnimation(prototype.lua().get("structure").get("direction_out").get("sheet")).get();
	}
}
