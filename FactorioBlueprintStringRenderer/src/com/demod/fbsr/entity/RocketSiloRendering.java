package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class RocketSiloRendering extends EntityRendererFactory {

	private SpriteDef protoBaseSprite;
	private SpriteDef protoShadowSprite;
	private SpriteDef protoDoorFrontSprite;
	private SpriteDef protoDoorBackSprite;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY, protoShadowSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY, protoDoorFrontSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY, protoDoorBackSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY2, protoBaseSprite, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBaseSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("base_day_sprite")).get();
		protoShadowSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("shadow_sprite")).get();
		protoDoorFrontSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("door_front_sprite")).get();
		protoDoorBackSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("door_back_sprite")).get();
	}
}
