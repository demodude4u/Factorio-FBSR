package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

public class FPRotatedSpriteFrame {
	public final int width;
	public final int height;
	public final int x;
	public final int y;
	public final FPVector shift;

	public FPRotatedSpriteFrame(LuaValue lua, int defaultWidth, int defaultHeight) {
		width = lua.get("width").optint(defaultWidth);
		height = lua.get("height").optint(defaultHeight);
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		shift = new FPVector(lua.get("shift"));
	}
}
