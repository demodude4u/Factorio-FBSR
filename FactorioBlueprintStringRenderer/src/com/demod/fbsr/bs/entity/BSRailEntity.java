package com.demod.fbsr.bs.entity;

import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.demod.fbsr.map.MapRail;

import org.json.JSONObject;

public class BSRailEntity extends BSEntity {

    private MapRail rail;

    public BSRailEntity(JSONObject json) {
        super(json);
    }

    public BSRailEntity(LegacyBlueprintEntity legacy) {
        super(legacy);
    }

    public void setRail(MapRail rail) {
        this.rail = rail;
    }

    public MapRail getRail() {
        return rail;
    }
}
