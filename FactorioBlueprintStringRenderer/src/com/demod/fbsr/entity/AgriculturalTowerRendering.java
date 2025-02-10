package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPWorkingVisualisations;

public class AgriculturalTowerRendering extends SimpleEntityRendering<BSEntity> {

	private FPWorkingVisualisations protoGraphicsSet;

	// TODO the crane

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		super.createRenderers(register, map, entity);

		register.accept(RenderUtils.spriteRenderer(protoGraphicsSet.createSprites(data, entity.direction, 0), entity,
				protoSelectionBox));
	}

	@Override
	public void defineEntity(SimpleEntityRendering<BSEntity>.Bindings bind, LuaTable lua) {
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(prototype.lua().get("graphics_set"));
	}

}
