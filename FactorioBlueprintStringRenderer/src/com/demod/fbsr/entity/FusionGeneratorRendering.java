package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public class FusionGeneratorRendering extends SimpleEntityRendering {
	private static final int FRAME = 0;

	private FPAnimation protoNorthAnimation;
	private FPAnimation protoEastAnimation;
	private FPAnimation protoSouthAnimation;
	private FPAnimation protoWestAnimation;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Direction dir = entity.getDirection();

		Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);

		if (dir == Direction.NORTH) {
			protoNorthAnimation.defineSprites(entityRegister, FRAME);
		} else if (dir == Direction.EAST) {
			protoEastAnimation.defineSprites(entityRegister, FRAME);
		} else if (dir == Direction.SOUTH) {
			protoSouthAnimation.defineSprites(entityRegister, FRAME);
		} else if (dir == Direction.WEST) {
			protoWestAnimation.defineSprites(entityRegister, FRAME);
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoNorthAnimation.defineSprites(register, FRAME);
		protoEastAnimation.defineSprites(register, FRAME);
		protoSouthAnimation.defineSprites(register, FRAME);
		protoWestAnimation.defineSprites(register, FRAME);
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
