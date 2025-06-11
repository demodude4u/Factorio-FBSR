package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Profile;

public class FPAnimationElement {
	public final FPAnimation animation;

	public FPAnimationElement(Profile profile, LuaValue lua) {
		animation = new FPAnimation(profile, lua.get("animation"));
	}
}
