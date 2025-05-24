package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
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

	public final Optional<FPSprite16Way> frontRailEndings;
	public final Optional<FPSprite16Way> backRailEndings;
	
	private final ImmutableList<FPRailPieceLayers> dirs;

	public FPRailPictureSet(ModsProfile profile, LuaValue lua) {
		north = new FPRailPieceLayers(profile, lua.get("north"));
		northeast = new FPRailPieceLayers(profile, lua.get("northeast"));
		east = new FPRailPieceLayers(profile, lua.get("east"));
		southeast = new FPRailPieceLayers(profile, lua.get("southeast"));
		south = new FPRailPieceLayers(profile, lua.get("south"));
		southwest = new FPRailPieceLayers(profile, lua.get("southwest"));
		west = new FPRailPieceLayers(profile, lua.get("west"));
		northwest = new FPRailPieceLayers(profile, lua.get("northwest"));

		Optional<FPSprite16Way> railEndings = FPUtils.opt(profile, lua.get("rail_endings"), FPSprite16Way::new);
		frontRailEndings = FPUtils.opt(profile, lua.get("front_rail_endings"), FPSprite16Way::new).or(() -> railEndings);
		backRailEndings = FPUtils.opt(profile, lua.get("back_rail_endings"), FPSprite16Way::new).or(() -> railEndings);

		dirs = ImmutableList.of(north, northeast, east, southeast, south, southwest, west, northwest);
	}

	public FPRailPieceLayers get(Direction direction) {
		return dirs.get(direction.ordinal());
	}

	public void getDefs(Consumer<ImageDef> register, int variation) {
		frontRailEndings.ifPresent(fp -> fp.getDefs(register));
		backRailEndings.ifPresent(fp -> fp.getDefs(register));
		dirs.forEach(fp -> fp.getDefs(register, variation));
	}
}