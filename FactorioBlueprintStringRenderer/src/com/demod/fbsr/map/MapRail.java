package com.demod.fbsr.map;

import java.util.Optional;

import com.demod.fbsr.entity.RailRendering.RailDef;

public class MapRail {
    private final MapPosition pos;
    private final RailDef def;

    public MapRail(MapPosition pos, RailDef def) {
        this.pos = pos;
        this.def = def;
    }

    public MapPosition getPos() {
        return pos;
    }

    public RailDef getDef() {
        return def;
    }
}