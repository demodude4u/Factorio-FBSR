package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public abstract class EntityWithHealthRendering extends EntityRendering {

    @Override
    public void defineEntity(Bindings bind, LuaTable lua) {
        Layer protoIntegrationPatchRenderLayer;
        Optional<FPSprite4Way> protoIntegrationPatch = Optional.empty();

        protoIntegrationPatchRenderLayer = FPUtils.optLayer(prototype.lua().get("integration_patch_render_layer")).orElse(Layer.LOWER_OBJECT);
        LuaValue luaGraphicsSet = prototype.lua().get("graphics_set");
        if (!luaGraphicsSet.isnil()) {
            protoIntegrationPatchRenderLayer = FPUtils.optLayer(luaGraphicsSet.get("integration_patch_render_layer")).orElse(protoIntegrationPatchRenderLayer);
        
           protoIntegrationPatch = FPUtils.opt(profile, luaGraphicsSet.get("integration_patch"), FPSprite4Way::new);
        }

        Optional<FPSprite4Way> protoIntegrationPatchOverride = FPUtils.opt(profile, prototype.lua().get("integration_patch"), FPSprite4Way::new);
        if (luaGraphicsSet.isnil() || protoIntegrationPatchOverride.isPresent()) {
            protoIntegrationPatch = protoIntegrationPatchOverride;
        }

        if (protoIntegrationPatch.isPresent()) {
            bind.sprite4Way(protoIntegrationPatch.get()).layer(protoIntegrationPatchRenderLayer);            
        }
    }

}
