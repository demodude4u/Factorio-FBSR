package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPMaterialTextureParameters {
	public final int count;
	public final String picture;
	public final double scale;
	public final int x;
	public final int y;
	public final int lineLength;

	public FPMaterialTextureParameters(LuaValue lua) {
		count = lua.get("count").checkint();
		picture = lua.get("picture").checkjstring();
		scale = lua.get("scale").optdouble(1.0) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		lineLength = lua.get("line_length").optint(0);
	}
}
