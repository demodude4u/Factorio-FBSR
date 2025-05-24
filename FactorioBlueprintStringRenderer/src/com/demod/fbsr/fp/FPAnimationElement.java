package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.ModsProfile;

public class FPAnimationElement {
	public final FPAnimation animation;

	public FPAnimationElement(ModsProfile profile, LuaValue lua) {
		animation = new FPAnimation(profile, lua.get("animation"));
	}
}
