package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation;
import com.google.common.collect.ImmutableList;

public class FusionGeneratorRendering extends SimpleEntityRendering<BSEntity> {

	private FPAnimation protoNorthAnimation;
	private FPAnimation protoEastAnimation;
	private FPAnimation protoSouthAnimation;
	private FPAnimation protoWestAnimation;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		super.createRenderers(register, map, entity);

		Direction dir = entity.direction;

		List<Sprite> sprites = ImmutableList.of();
		if (dir == Direction.NORTH) {
			sprites = protoNorthAnimation.createSprites(data, 0);
		} else if (dir == Direction.EAST) {
			sprites = protoEastAnimation.createSprites(data, 0);
		} else if (dir == Direction.SOUTH) {
			sprites = protoSouthAnimation.createSprites(data, 0);
		} else if (dir == Direction.WEST) {
			sprites = protoWestAnimation.createSprites(data, 0);
		}
		register.accept(RenderUtils.spriteRenderer(sprites, entity, drawBounds));
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaGraphicsSet = prototype.lua().get("graphics_set");
		protoNorthAnimation = new FPAnimation(luaGraphicsSet.get("north_graphics_set").get("animation"));
		protoEastAnimation = new FPAnimation(luaGraphicsSet.get("east_graphics_set").get("animation"));
		protoSouthAnimation = new FPAnimation(luaGraphicsSet.get("south_graphics_set").get("animation"));
		protoWestAnimation = new FPAnimation(luaGraphicsSet.get("west_graphics_set").get("animation"));
	}

	// TODO connectors between adjacent fusion reactors and generators
	// https://factorio.com/blog/post/fff-420

}
