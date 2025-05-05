package com.demod.fbsr.map;

import com.demod.fbsr.MapUtils;

public class MapPosition3D {
    public static final double ELEVATED_HEIGHT = 3.0;
    public static final int ELEVATED_HEIGHT_FP = MapUtils.unitToFixedPoint(ELEVATED_HEIGHT);

    private final MapPosition pos;
    private final int heightfp;

    private MapPosition3D(MapPosition pos, int heightfp) {
        this.pos = pos;
        this.heightfp = heightfp;
    }

    public MapPosition get2D() {
        return pos;
    }

    public double getHeight() {
        return MapUtils.fixedPointToUnit(heightfp);
    }

    public boolean isElevated() {
        return heightfp == ELEVATED_HEIGHT_FP;
    }

    public boolean isGround() {
        return heightfp == 0;
    }

    public static MapPosition3D by2DUnit(MapPosition pos, double height) {
        return new MapPosition3D(pos, MapUtils.unitToFixedPoint(height));
    }

    public static MapPosition3D by2DGround(MapPosition pos) {
        return new MapPosition3D(pos, 0);
    }

    public static MapPosition3D by2DElevated(MapPosition pos) {
        return new MapPosition3D(pos, ELEVATED_HEIGHT_FP);
    }

    public static MapPosition3D byUnit(double x, double y, double height) {
        return new MapPosition3D(MapPosition.byUnit(x, y), MapUtils.unitToFixedPoint(height));
    }

    public static MapPosition3D byGroundUnit(double x, double y) {
        return new MapPosition3D(MapPosition.byUnit(x, y), 0);
    }

    public static MapPosition3D byElevatedUnit(double x, double y) {
        return new MapPosition3D(MapPosition.byUnit(x, y), ELEVATED_HEIGHT_FP);
    }

    public MapPosition3D addUnit(double x, double y, double height) {
        return new MapPosition3D(pos.add(MapPosition.byUnit(x, y)), heightfp + MapUtils.unitToFixedPoint(height));
    }

    public MapPosition flatten() {
        return new MapPosition(pos.xfp, pos.yfp - heightfp);
    }

    public MapPosition3D add2D(MapPosition pos) {
        return new MapPosition3D(this.pos.add(pos), heightfp);
    }

    public MapPosition3D subtract2D(MapPosition pos) {
        return new MapPosition3D(this.pos.subtract(pos), heightfp);
    }

    public double distance(MapPosition3D other) {
        int dxfp = other.pos.xfp - pos.xfp;
        int dyfp = other.pos.yfp - pos.yfp;
        int dhfp = other.heightfp - heightfp;
        return MapUtils.fixedPointToUnit((int) Math.round(Math.sqrt(dxfp * dxfp + dyfp * dyfp + dhfp * dhfp)));
    }

    public static MapPosition3D average(MapPosition3D p1, MapPosition3D p2) {
        return new MapPosition3D(MapPosition.average(p1.pos, p2.pos), (p1.heightfp + p2.heightfp) / 2);
    }
}