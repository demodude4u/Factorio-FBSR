package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;

@EntityType("proxy-container")
public class ProxyContainerRendering extends EntityWithOwnerRendering {
    @Override
    public void defineEntity(Bindings bind, LuaTable lua) {
        super.defineEntity(bind, lua);

        bind.sprite(lua.get("picture"));
    }
}
