package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;

public class FPHeatConnection {

    public final FPVector position;
    public final Direction direction;

    public FPHeatConnection(LuaValue lua) {
        position = new FPVector(lua.get("position"));
        direction = FPUtils.direction(lua.get("direction"));
    }
}
