package com.demod.fbsr.fp;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.ImageDef;
import com.google.common.collect.ImmutableList;

public class FPRailPictureSet {
	public final FPRailPieceLayers north;
	public final FPRailPieceLayers northeast;
	public final FPRailPieceLayers east;
	public final FPRailPieceLayers southeast;
	public final FPRailPieceLayers south;
	public final FPRailPieceLayers southwest;
	public final FPRailPieceLayers west;
	public final FPRailPieceLayers northwest;

	private final ImmutableList<FPRailPieceLayers> dirs;

	public FPRailPictureSet(LuaValue lua) {
		north = new FPRailPieceLayers(lua.get("north"));
		northeast = new FPRailPieceLayers(lua.get("northeast"));
		east = new FPRailPieceLayers(lua.get("east"));
		southeast = new FPRailPieceLayers(lua.get("southeast"));
		south = new FPRailPieceLayers(lua.get("south"));
		southwest = new FPRailPieceLayers(lua.get("southwest"));
		west = new FPRailPieceLayers(lua.get("west"));
		northwest = new FPRailPieceLayers(lua.get("northwest"));

		dirs = ImmutableList.of(north, northeast, east, southeast, south, southwest, west, northwest);
	}

	public FPRailPieceLayers get(Direction direction) {
		return dirs.get(direction.ordinal());
	}

	public void getDefs(Consumer<ImageDef> register, int variation) {
		dirs.forEach(fp -> fp.getDefs(register, variation));
	}
}