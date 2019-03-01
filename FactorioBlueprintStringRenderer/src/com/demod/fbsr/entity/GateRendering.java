package com.demod.fbsr.entity;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class GateRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		boolean vertical = isVerticalGate(entity);

		String orientation = vertical ? "vertical" : "horizontal";

		LuaValue spriteLayersLua = prototype.lua().get(orientation + "_animation").get("layers");

		Sprite spriteShadow = RenderUtils.getSpriteFromAnimation(spriteLayersLua.get(2));
		register.accept(RenderUtils.spriteRenderer(spriteShadow, entity, prototype));

		Sprite sprite = RenderUtils.getSpriteFromAnimation(spriteLayersLua.get(1));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
	}

	private boolean isVerticalGate(BlueprintEntity entity) {
		return entity.getDirection().cardinal() % 2 == 0;
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		if (isVerticalGate(entity)) {
			map.setVerticalGate(entity.getPosition());
		} else {
			map.setHorizontalGate(entity.getPosition());
		}
	}
}
