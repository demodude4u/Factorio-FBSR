package com.demod.fbsr;

import java.awt.Point;
import java.awt.Rectangle;

public class AtlasRef {
    private boolean valid = false;
    private int packageId = -1;
    private int atlasId = -1;
    private Rectangle rect = null;
    private Point trim = null;

    public AtlasRef() {
    }

    public boolean isValid() {
        return valid;
    }

    public void set(int packageId, int atlasId, Rectangle rect, Point trim) {
        valid = true;
        this.packageId = packageId;
        this.atlasId = atlasId;
        this.rect = rect;
        this.trim = trim;
    }

    public void link(AtlasRef shared) {
        valid = shared.valid;
        packageId = shared.packageId;
        atlasId = shared.atlasId;
        rect = shared.rect;
        trim = shared.trim;
    }

    public void reset() {
        valid = false;
        packageId = -1;
        atlasId = -1;
        rect = null;
        trim = null;
    }

    public int getPackageId() {
        return packageId;
    }

    public int getAtlasId() {
        return atlasId;
    }

    public Rectangle getRect() {
        return rect;
    }

    public Point getTrim() {
        return trim;
    }
}