package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;

public class FPWorkingVisualisations {
	public final FPAnimation4Way animation;
	public final Optional<FPAnimation4Way> idleAnimation;

	public FPWorkingVisualisations(LuaValue lua) {
		animation = new FPAnimation4Way(lua.get("animation"));
		idleAnimation = FPUtils.opt(lua.get("idle_animation"), FPAnimation4Way::new);
	}
}
