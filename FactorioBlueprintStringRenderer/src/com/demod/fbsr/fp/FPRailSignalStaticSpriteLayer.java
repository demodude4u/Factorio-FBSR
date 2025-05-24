package com.demod.fbsr.fp;

import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;

public class FPRailSignalStaticSpriteLayer {
	public final FPAnimation sprites;
	public final List<Integer> alignToFrameIndex;

	public FPRailSignalStaticSpriteLayer(ModsProfile profile, LuaValue lua) {
		sprites = new FPAnimation(profile, lua.get("sprites"));
		alignToFrameIndex = FPUtils.list(lua.get("align_to_frame_index"), LuaValue::toint);
	}
}