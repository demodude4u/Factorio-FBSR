package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

@EntityType("fusion-generator")
public class FusionGeneratorRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		LuaValue luaGraphicsSet = prototype.lua().get("graphics_set");
		bind.animation(luaGraphicsSet.get("north_graphics_set").get("animation"))
				.conditional((map, entity) -> entity.getDirection() == Direction.NORTH);
		bind.animation(luaGraphicsSet.get("east_graphics_set").get("animation"))
				.conditional((map, entity) -> entity.getDirection() == Direction.EAST);
		bind.animation(luaGraphicsSet.get("south_graphics_set").get("animation"))
				.conditional((map, entity) -> entity.getDirection() == Direction.SOUTH);
		bind.animation(luaGraphicsSet.get("west_graphics_set").get("animation"))
				.conditional((map, entity) -> entity.getDirection() == Direction.WEST);

		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}
}
