package com.demod.fbsr;

import java.io.File;

import com.demod.factorio.FactorioData;

public class ModsProfile {
    private File folderData;
    private FactorioData data;
    private AtlasPackage atlasPackage;

    public ModsProfile(File folderData, FactorioData data, AtlasPackage atlasPackage) {
        this.folderData = folderData;
        this.data = data;
        this.atlasPackage = atlasPackage;
    }

    public File getFolderData() {
        return folderData;
    }

    public FactorioData getData() {
        return data;
    }

    public AtlasPackage getAtlasPackage() {
        return atlasPackage;
    }
}
