package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPTileSpriteLayoutVariant {
	public final String spritesheet;
	public final double scale;
	public final int x;
	public final int y;
	public final int tileHeight;
	public final int lineLength;
	public final int count;

	public FPTileSpriteLayoutVariant(LuaValue lua) {
		spritesheet = lua.get("spritesheet").tojstring();
		scale = lua.get("scale").optdouble(1.0) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		tileHeight = lua.get("tile_height").optint(1);
		lineLength = lua.get("line_length").optint(0);
		count = lua.get("count").checkint();
	}
}
