package com.demod.fbsr.entity;

import java.awt.Color;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class SpiderVehicleRendering extends EntityRendererFactory<BSEntity> {

	// TODO rendering spider hard, just use icon for now
	private String protoIcon;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {

		// XXX this is hard-coded and wrong
		Sprite sprite = RenderUtils.createSprite(protoIcon, false, "normal", Color.white, 0, 0, 64, 64, -1, -1, 2);
		register.accept(RenderUtils.spriteRenderer(sprite, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		protoIcon = prototype.lua().get("icon").checkjstring();
	}

}
