package com.demod.fbsr.fp;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;

public class FPCargoBayConnections {
	public final FPLayeredSpriteVariations topWall;
	public final FPLayeredSpriteVariations rightWall;
	public final FPLayeredSpriteVariations bottomWall;
	public final FPLayeredSpriteVariations leftWall;
	public final FPLayeredSpriteVariations topLeftOuterCorner;
	public final FPLayeredSpriteVariations topRightOuterCorner;
	public final FPLayeredSpriteVariations bottomLeftOuterCorner;
	public final FPLayeredSpriteVariations bottomRightOuterCorner;
	public final FPLayeredSpriteVariations topLeftInnerCorner;
	public final FPLayeredSpriteVariations topRightInnerCorner;
	public final FPLayeredSpriteVariations bottomLeftInnerCorner;
	public final FPLayeredSpriteVariations bottomRightInnerCorner;
	public final FPLayeredSpriteVariations bridgeHorizontalNarrow;
	public final FPLayeredSpriteVariations bridgeHorizontalWide;
	public final FPLayeredSpriteVariations bridgeVerticalNarrow;
	public final FPLayeredSpriteVariations bridgeVerticalWide;
	public final FPLayeredSpriteVariations bridgeCrossing;

	public FPCargoBayConnections(Profile profile, LuaValue lua) {
		topWall = new FPLayeredSpriteVariations(profile, lua.get("top_wall"));
		rightWall = new FPLayeredSpriteVariations(profile, lua.get("right_wall"));
		bottomWall = new FPLayeredSpriteVariations(profile, lua.get("bottom_wall"));
		leftWall = new FPLayeredSpriteVariations(profile, lua.get("left_wall"));
		topLeftOuterCorner = new FPLayeredSpriteVariations(profile, lua.get("top_left_outer_corner"));
		topRightOuterCorner = new FPLayeredSpriteVariations(profile, lua.get("top_right_outer_corner"));
		bottomLeftOuterCorner = new FPLayeredSpriteVariations(profile, lua.get("bottom_left_outer_corner"));
		bottomRightOuterCorner = new FPLayeredSpriteVariations(profile, lua.get("bottom_right_outer_corner"));
		topLeftInnerCorner = new FPLayeredSpriteVariations(profile, lua.get("top_left_inner_corner"));
		topRightInnerCorner = new FPLayeredSpriteVariations(profile, lua.get("top_right_inner_corner"));
		bottomLeftInnerCorner = new FPLayeredSpriteVariations(profile, lua.get("bottom_left_inner_corner"));
		bottomRightInnerCorner = new FPLayeredSpriteVariations(profile, lua.get("bottom_right_inner_corner"));
		bridgeHorizontalNarrow = new FPLayeredSpriteVariations(profile, lua.get("bridge_horizontal_narrow"));
		bridgeHorizontalWide = new FPLayeredSpriteVariations(profile, lua.get("bridge_horizontal_wide"));
		bridgeVerticalNarrow = new FPLayeredSpriteVariations(profile, lua.get("bridge_vertical_narrow"));
		bridgeVerticalWide = new FPLayeredSpriteVariations(profile, lua.get("bridge_vertical_wide"));
		bridgeCrossing = new FPLayeredSpriteVariations(profile, lua.get("bridge_crossing"));
	}

	public void getDefs(Consumer<ImageDef> register) {
		topWall.getDefs(register);
		rightWall.getDefs(register);
		bottomWall.getDefs(register);
		leftWall.getDefs(register);
		topLeftOuterCorner.getDefs(register);
		topRightOuterCorner.getDefs(register);
		bottomLeftOuterCorner.getDefs(register);
		bottomRightOuterCorner.getDefs(register);
		topLeftInnerCorner.getDefs(register);
		topRightInnerCorner.getDefs(register);
		bottomLeftInnerCorner.getDefs(register);
		bottomRightInnerCorner.getDefs(register);
		bridgeHorizontalNarrow.getDefs(register);
		bridgeHorizontalWide.getDefs(register);
		bridgeVerticalNarrow.getDefs(register);
		bridgeVerticalWide.getDefs(register);
		bridgeCrossing.getDefs(register);
	}
}