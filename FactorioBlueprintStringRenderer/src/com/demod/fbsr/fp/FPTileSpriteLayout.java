package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPTileSpriteLayout {
	public final String picture;
	public final int count;
	public final int lineLength;
	public final double scale;
	public final int x;
	public final int y;

	public FPTileSpriteLayout(LuaValue lua) {
		picture = lua.get("picture").tojstring();
		count = lua.get("count").optint(0);
		lineLength = lua.get("line_length").optint(0);
		scale = lua.get("scale").optdouble(1) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
	}
}