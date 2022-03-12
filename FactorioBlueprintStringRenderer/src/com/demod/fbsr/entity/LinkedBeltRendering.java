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
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class LinkedBeltRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		Sprite beltSprite = TransportBeltRendering.getBeltSprite(prototype, entity.getDirection(), BeltBend.NONE);

		Sprite sprite = RenderUtils.getSpriteFromAnimation(
				prototype.lua().get("structure").get(input ? "direction_in" : "direction_out").get("sheet"));
		sprite.source.x += sprite.source.width * (structDir.cardinal());

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, beltSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, sprite, entity, prototype));
	}
}
