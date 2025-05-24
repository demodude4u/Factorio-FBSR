package com.demod.fbsr.fp;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.SpriteDef;

public class FPBeaconGraphicsSet {
	public final List<FPAnimationElement> animationList;

	public FPBeaconGraphicsSet(ModsProfile profile, LuaValue lua) {
		animationList = FPUtils.list(profile, lua.get("animation_list"), FPAnimationElement::new);
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int frame) {
		for (FPAnimationElement element : animationList) {
			element.animation.defineSprites(consumer, frame);
		}
	}
}