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

public class RoboportRendering extends EntityRendererFactory {

	private List<SpriteDef> protoBase;
	private SpriteDef protoDoorDown;
	private SpriteDef protoDoorUp;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDefRenderer(protoBase, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDefRenderer(protoDoorDown, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDefRenderer(protoDoorUp, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBase = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base"));
		protoDoorDown = RenderUtils.getSpriteFromAnimation(prototype.lua().get("door_animation_down")).get();
		protoDoorUp = RenderUtils.getSpriteFromAnimation(prototype.lua().get("door_animation_up")).get();
	}
}
