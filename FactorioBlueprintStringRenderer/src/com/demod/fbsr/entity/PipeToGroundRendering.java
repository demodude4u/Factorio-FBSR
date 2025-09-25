package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.map.MapEntity;

@EntityType("pipe-to-ground")
public class PipeToGroundRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.sprite4Way(lua.get("pictures"));
		bind.fluidBox(lua.get("fluid_box")).ignorePipeCovers();
	}
}
