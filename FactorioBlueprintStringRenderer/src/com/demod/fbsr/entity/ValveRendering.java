package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;

@EntityType("valve")
public class ValveRendering extends EntityWithOwnerRendering {
    @Override
    public void defineEntity(Bindings bind, LuaTable lua) {
        super.defineEntity(bind, lua);

        bind.animation4Way(lua.get("animations"));
		bind.fluidBox(lua.get("fluid_box"));
    }

}
