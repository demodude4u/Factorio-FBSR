package com.demod.fbsr.fp;

import java.awt.geom.Point2D;

import org.luaj.vm2.LuaValue;

public class FPVector {

	public final double x;
	public final double y;

	public FPVector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public FPVector(LuaValue lua) {
		LuaValue luaX = lua.get("x");
		LuaValue luaY = lua.get("y");
		if (!luaX.isnil() || !luaY.isnil()) {
			x = luaX.checkdouble();
			y = luaY.checkdouble();
		} else {
			x = lua.get(1).checkdouble();
			y = lua.get(2).checkdouble();
		}
	}

	public Point2D.Double createPoint() {
		return new Point2D.Double(x, y);
	}

}
