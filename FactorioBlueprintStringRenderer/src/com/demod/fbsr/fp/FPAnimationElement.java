package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPAnimationElement {
	public final FPAnimation animation;

	public FPAnimationElement(LuaValue lua) {
		animation = new FPAnimation(lua.get("animation"));
	}
}
