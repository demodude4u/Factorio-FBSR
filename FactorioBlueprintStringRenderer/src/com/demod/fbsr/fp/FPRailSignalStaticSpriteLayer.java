package com.demod.fbsr.fp;

import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPRailSignalStaticSpriteLayer {
	public final FPAnimation sprites;
	public final List<Integer> alignToFrameIndex;

	public FPRailSignalStaticSpriteLayer(LuaValue lua) {
		sprites = new FPAnimation(lua.get("sprites"));
		alignToFrameIndex = FPUtils.list(lua.get("align_to_frame_index"), LuaValue::toint);
	}
}