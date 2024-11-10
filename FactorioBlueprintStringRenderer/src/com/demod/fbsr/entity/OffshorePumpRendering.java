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

public class OffshorePumpRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue graphicsSet = prototype.lua().get("graphics_set");
		if (!graphicsSet.isnil()) {
			protoDirSprites = RenderUtils.getDirSpritesFromAnimation(graphicsSet.get("animation"));
		} else {
			protoDirSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("picture"));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setPipe(entity.getPosition(), entity.getDirection().back());
	}
}
