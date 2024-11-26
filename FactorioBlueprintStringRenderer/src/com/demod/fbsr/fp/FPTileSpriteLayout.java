package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

public class FPTileSpriteLayout {
	public final String picture;
	public final int count;
	public final int lineLength;
	public final int scale;
	public final int x;
	public final int y;

	public FPTileSpriteLayout(LuaValue lua) {
		picture = lua.get("picture").tojstring();
		count = lua.get("count").optint(0);
		lineLength = lua.get("line_length").optint(0);
		scale = lua.get("scale").optint(1) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
	}
}