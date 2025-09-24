package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;

@EntityType("unit-spawner")
public class EnemySpawnerRendering extends EntityWithOwnerRendering {

    @Override
    public void defineEntity(Bindings bind, LuaTable lua) {
        super.defineEntity(bind, lua);

        bind.animationVariations(lua.get("graphics_set").get("animations"));
    }
}
