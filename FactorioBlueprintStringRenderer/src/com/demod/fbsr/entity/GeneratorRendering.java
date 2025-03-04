package com.demod.fbsr.entity;

import java.util.function.Consumer;

import javax.swing.Renderer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation;

public class GeneratorRendering extends SimpleEntityRendering {

	private FPAnimation protoVerticalAnimation;
	private FPAnimation protoHorizontalAnimation;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		super.createRenderers(register, map, entity);

		if (isVertical(entity)) {
			register.accept(RenderUtils.spriteRenderer(protoVerticalAnimation.createSprites(data, 0), entity,
					drawBounds));
		} else {
			register.accept(RenderUtils.spriteRenderer(protoHorizontalAnimation.createSprites(data, 0), entity,
					drawBounds));
		}
	}

	@Override
	public void defineEntity(SimpleEntityRendering.Bindings bind, LuaTable lua) {
		bind.fluidBox(lua.get("fluid_box"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoVerticalAnimation = new FPAnimation(prototype.lua().get("vertical_animation"));
		protoHorizontalAnimation = new FPAnimation(prototype.lua().get("horizontal_animation"));
	}

	private boolean isVertical(BSEntity entity) {
		return entity.direction.cardinal() % 2 == 0;
	}
}
