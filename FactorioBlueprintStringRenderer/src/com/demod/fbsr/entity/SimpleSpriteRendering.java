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
import com.demod.fbsr.fp.FPSprite;

public abstract class SimpleSpriteRendering extends EntityRendererFactory {

	private FPSprite protoSprite;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteRenderer(protoSprite.createSprites(), entity, protoSelectionBox));
	}

	public abstract FPSprite getSprite(LuaValue lua);

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		protoSprite = getSprite(prototype.lua());
	}

}
