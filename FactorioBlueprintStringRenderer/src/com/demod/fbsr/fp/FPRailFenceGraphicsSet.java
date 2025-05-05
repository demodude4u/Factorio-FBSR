package com.demod.fbsr.fp;

import java.util.function.Consumer;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.def.ImageDef;

public class FPRailFenceGraphicsSet {
    public final int segmentCount;
    public final FPRailFencePictureSet sideA;
    public final FPRailFencePictureSet sideB;
    public final Layer backFenceRenderLayer;
    public final Layer backFenceRenderLayerSecondary;
    public final Layer frontFenceRenderLayer;
    public final Layer frontFenceRenderLayerSecondary;
    
    public FPRailFenceGraphicsSet(LuaValue lua) {
        segmentCount = lua.get("segment_count").checkint();
        sideA = new FPRailFencePictureSet(lua.get("side_A"));
        sideB = new FPRailFencePictureSet(lua.get("side_B"));
        backFenceRenderLayer = FPUtils.optLayer(lua.get("back_fence_render_layer")).orElse(Layer.ELEVATED_LOWER_OBJECT);
        backFenceRenderLayerSecondary = FPUtils.optLayer(lua.get("back_fence_render_layer_secondary")).orElse(Layer.ELEVATED_LOWER_OBJECT);
        frontFenceRenderLayer = FPUtils.optLayer(lua.get("front_fence_render_layer")).orElse(Layer.ELEVATED_HIGHER_OBJECT);
        frontFenceRenderLayerSecondary = FPUtils.optLayer(lua.get("front_fence_render_layer_secondary")).orElse(Layer.ELEVATED_HIGHER_OBJECT);
    }
    
    public void getDefs(Consumer<ImageDef> register) {
        sideA.getDefs(register);
        sideB.getDefs(register);
    }
}
