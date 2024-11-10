package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class PowerSwitchRendering extends EntityRendererFactory {

	private List<SpriteDef> protoSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		register.accept(RenderUtils.spriteDefRenderer(protoSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("power_on_animation"));
	}
}
