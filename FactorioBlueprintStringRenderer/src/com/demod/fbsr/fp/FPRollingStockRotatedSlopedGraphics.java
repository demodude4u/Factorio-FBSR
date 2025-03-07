package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPRollingStockRotatedSlopedGraphics {
	public final FPRotatedSprite rotated;
	public final double slopeAngleBetweenFrames;
	public final boolean slopeBackEqualsFront;
	public final FPRotatedSprite sloped;

	public FPRollingStockRotatedSlopedGraphics(LuaValue lua) {
		rotated = new FPRotatedSprite(lua.get("rotated"), 16);
		slopeAngleBetweenFrames = lua.get("slope_angle_between_frames").optdouble(1.333);
		slopeBackEqualsFront = lua.get("slope_back_equals_front").optboolean(false);
		sloped = new FPRotatedSprite(lua.get("sloped"), 16);
	}
}