package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPTileTransitions {
	public final Optional<String> spritesheet;
	public final FPTileTransitionSpritesheetLayout layout;

	public FPTileTransitions(LuaValue lua) {
		spritesheet = FPUtils.optString(lua.get("spritesheet"));

		boolean overlayEnabled = lua.get("overlay_enabled").optboolean(true);
		Optional<String> overlaySpritesheet = FPUtils.optString(lua.get("overlay_spritesheet"));
		LuaValue overlayLayoutLua = lua.get("overlay_layout");

		boolean maskEnabled = lua.get("mask_enabled").optboolean(true);
		Optional<String> maskSpritesheet = FPUtils.optString(lua.get("mask_spritesheet"));
		LuaValue maskLayoutLua = lua.get("mask_layout");

		boolean backgroundEnabled = lua.get("background_enabled").optboolean(true);
		Optional<String> backgroundSpritesheet = FPUtils.optString(lua.get("background_spritesheet"));
		LuaValue backgroundLayoutLua = lua.get("background_layout");

		layout = new FPTileTransitionSpritesheetLayout(lua.get("layout"), spritesheet, //
				overlayEnabled, overlaySpritesheet, overlayLayoutLua, //
				maskEnabled, maskSpritesheet, maskLayoutLua, //
				backgroundEnabled, backgroundSpritesheet, backgroundLayoutLua);
	}
}
