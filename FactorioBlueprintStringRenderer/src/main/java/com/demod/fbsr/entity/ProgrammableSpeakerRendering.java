package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;

public class ProgrammableSpeakerRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("sprite"));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY3, sprites, entity, prototype));
	}
}
