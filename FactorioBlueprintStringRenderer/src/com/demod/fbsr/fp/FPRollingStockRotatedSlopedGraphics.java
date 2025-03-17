package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPRollingStockRotatedSlopedGraphics {
	public final FPRotatedSprite rotated;
	public final double slopeAngleBetweenFrames;
	public final boolean slopeBackEqualsFront;
	public final Optional<FPRotatedSprite> sloped;

	public FPRollingStockRotatedSlopedGraphics(LuaValue lua) {
		rotated = new FPRotatedSprite(lua.get("rotated"), 16);
		slopeAngleBetweenFrames = lua.get("slope_angle_between_frames").optdouble(1.333);
		slopeBackEqualsFront = lua.get("slope_back_equals_front").optboolean(false);
		sloped = FPUtils.opt(lua.get("sloped"), l -> new FPRotatedSprite(l, 16));
	}
}