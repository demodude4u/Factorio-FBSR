package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

public class FPAnimationParameters extends FPSpriteParameters {

	public final int frameCount;
	public final int lineLength;

	public FPAnimationParameters(LuaValue lua) {
		super(lua);

		frameCount = lua.get("frame_count").optint(1);
		lineLength = lua.get("line_length").optint(0);
	}

}
