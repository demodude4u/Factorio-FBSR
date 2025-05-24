package com.demod.fbsr;

import com.demod.factorio.FactorioData;

public class ModsProfile {
    private FactorioData data;
    private AtlasPackage atlasPackage;

    public ModsProfile(FactorioData data, AtlasPackage atlasPackage) {
        this.data = data;
        this.atlasPackage = atlasPackage;
    }

    public FactorioData getData() {
        return data;
    }

    public AtlasPackage getAtlasPackage() {
        return atlasPackage;
    }
}
