package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Profile;

public class FPBeltReaderLayer {
    public final Layer renderLayer;
    public final FPRotatedAnimation sprites;

    public FPBeltReaderLayer(Profile profile, LuaValue lua) {
        renderLayer = FPUtils.optLayer(lua.get("render_layer")).orElse(Layer.TRANSPORT_BELT_READER);
        sprites = new FPRotatedAnimation(profile, lua.get("sprites"));
    }
}
