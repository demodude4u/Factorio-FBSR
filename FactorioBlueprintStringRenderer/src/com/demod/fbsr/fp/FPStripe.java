package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

public class FPStripe {

	public final int widthInFrames;
	public final int heightInFrames;
	public final String filename;
	public final int x;
	public final int y;

	public FPStripe(LuaValue lua) {
		widthInFrames = lua.get("width_in_frames").checkint();
		heightInFrames = lua.get("height_in_frames").checkint();
		filename = lua.get("filename").toString();
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
	}
}
