package com.demod.fbsr.entity;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class BurnerGeneratorRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue idleAnimation = prototype.lua().get("idle_animation");
		if (!idleAnimation.isnil()) {
			protoDirSprites = RenderUtils.getDirSpritesFromAnimation(idleAnimation);
		} else {
			protoDirSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("animation"));
		}
	}
}
