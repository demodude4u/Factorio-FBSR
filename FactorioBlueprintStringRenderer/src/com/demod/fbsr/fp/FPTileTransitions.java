package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPTileTransitions {
	public final Optional<FPTileTransitionVariantLayout> backgroundLayout;
	public final Optional<FPTileTransitionVariantLayout> overlayLayout;
	public final Optional<FPTileTransitionVariantLayout> maskLayout;

	public FPTileTransitions(LuaValue lua) {
		backgroundLayout = FPUtils.opt(lua.get("background_layout"), FPTileTransitionVariantLayout::new);
		overlayLayout = FPUtils.opt(lua.get("overlay_layout"), FPTileTransitionVariantLayout::new);
		maskLayout = FPUtils.opt(lua.get("mask_layout"), FPTileTransitionVariantLayout::new);
	}
}
