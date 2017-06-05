package com.demod.fbsr.render;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class AmmoTurretRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite baseSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("base_picture").get("layers").get(1));
		register.accept(RenderUtils.spriteRenderer(baseSprite, entity, prototype));
		LuaValue turretLayers = prototype.lua().get("folded_animation").get("layers");
		Sprite turretSprite = RenderUtils.getSpriteFromAnimation(turretLayers.get(1));
		turretSprite.source.y += turretSprite.source.height * entity.getDirection().cardinal();
		register.accept(RenderUtils.spriteRenderer(turretSprite, entity, prototype));
	}
}
