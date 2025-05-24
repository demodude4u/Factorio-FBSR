package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;

public class FPTileTransitionSpritesheetLayout {

	public final FPTileSpriteLayoutDefaults defaults;

	public final Optional<FPTileTransitionVariantLayout> overlay;
	public final Optional<FPTileTransitionVariantLayout> mask;
	public final Optional<FPTileTransitionVariantLayout> background;

	public FPTileTransitionSpritesheetLayout(ModsProfile profile, LuaValue lua, Optional<String> defaultSpritesheet, boolean overlayEnabled,
			Optional<String> overlayOverrideSpritesheet, LuaValue overlayOverrideLayoutLua, boolean maskEnabled,
			Optional<String> maskOverrideSpritesheet, LuaValue maskOverrideLayoutLua, boolean backgroundEnabled,
			Optional<String> backgroundOverrideSpritesheet, LuaValue backgroundOverrideLayoutLua) {

		defaults = new FPTileSpriteLayoutDefaults(lua, "", false);

		if (overlayEnabled) {
			overlay = FPUtils.opt(overlayOverrideLayoutLua.isnil() ? lua.get("overlay") : overlayOverrideLayoutLua,
					l -> new FPTileTransitionVariantLayout(profile, l, overlayOverrideSpritesheet, defaults));
		} else {
			overlay = Optional.empty();
		}

		if (maskEnabled) {
			mask = FPUtils.opt(maskOverrideLayoutLua.isnil() ? lua.get("mask") : maskOverrideLayoutLua,
					l -> new FPTileTransitionVariantLayout(profile, l, maskOverrideSpritesheet, defaults));
		} else {
			mask = Optional.empty();
		}

		if (backgroundEnabled) {
			background = FPUtils.opt(
					backgroundOverrideLayoutLua.isnil() ? lua.get("background") : backgroundOverrideLayoutLua,
					l -> new FPTileTransitionVariantLayout(profile, l, backgroundOverrideSpritesheet, defaults));
		} else {
			background = Optional.empty();
		}
	}
}
