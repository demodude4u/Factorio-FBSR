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
import com.demod.fbsr.fp.FPSprite4Way;

public abstract class SimpleSprite4WayRendering extends EntityRendererFactory {

	private FPSprite4Way protoSprite;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteRenderer(protoSprite.createSprites(entity.getDirection()), entity,
				protoSelectionBox));
	}

	public abstract FPSprite4Way getSprite(LuaValue lua);

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		protoSprite = getSprite(prototype.lua());
	}

}
