package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

public class FPAnimationElement {
	public final FPAnimation animation;

	public FPAnimationElement(LuaValue lua) {
		animation = new FPAnimation(lua.get("animation"));
	}
}
