package com.demod.fbsr.fp;

import java.awt.geom.Rectangle2D;

import org.luaj.vm2.LuaValue;

public class FPBoundingBox {
	public final FPVector leftTop;
	public final FPVector rightBottom;

	public FPBoundingBox(double x1, double y1, double x2, double y2) {
		leftTop = new FPVector(x1, y1);
		rightBottom = new FPVector(x2, y2);
	}

	public FPBoundingBox(LuaValue lua) {
		// Technically these are MapPosition, but its the same as Vector
		LuaValue luaLeftTop = lua.get("left_top");
		LuaValue luaRightBottom = lua.get("right_bottom");
		if (!luaLeftTop.isnil() || !luaRightBottom.isnil()) {
			leftTop = new FPVector(luaLeftTop);
			rightBottom = new FPVector(luaRightBottom);
		} else {
			leftTop = new FPVector(lua.get(1));
			rightBottom = new FPVector(lua.get(2));
		}
	}

	public Rectangle2D.Double createRect() {
		return new Rectangle2D.Double(leftTop.x, leftTop.y, rightBottom.x - leftTop.x, rightBottom.y - leftTop.y);
	}

	public FPBoundingBox shift(FPVector v) {
		return new FPBoundingBox(leftTop.x + v.x, leftTop.y + v.y, rightBottom.x + v.x, rightBottom.y + v.y);
	}
}
