package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

@EntityType("generator")
public class GeneratorRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("vertical_animation")).conditional((map, entity) -> {
			return isVertical(entity);
		});
		bind.animation(lua.get("horizontal_animation")).conditional((map, entity) -> {
			return !isVertical(entity);
		});
		bind.fluidBox(lua.get("fluid_box"));
	}

	private boolean isVertical(MapEntity entity) {
		return entity.getDirection().cardinal() % 2 == 0;
	}
}
