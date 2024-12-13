package com.demod.fbsr.entity;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPWorkingVisualisations;

public class AgriculturalTowerRendering extends SimpleEntityRendering<BSEntity> {

	private FPWorkingVisualisations protoGraphicsSet;

	// TODO the crane

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		register.accept(RenderUtils.spriteRenderer(protoGraphicsSet.createSprites(entity.direction, 0), entity,
				protoSelectionBox));
	}

	@Override
	public void defineEntity(SimpleEntityRendering<BSEntity>.Bindings bind, LuaValue lua) {
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoGraphicsSet = new FPWorkingVisualisations(prototype.lua().get("graphics_set"));
	}

}
