package com.demod.fbsr.fp;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.SpriteDef;

public class FPBoilerPictureSet {
	public final FPBoilerPictures north;
	public final FPBoilerPictures east;
	public final FPBoilerPictures south;
	public final FPBoilerPictures west;

	public FPBoilerPictureSet(Profile profile, LuaValue lua) {
		north = new FPBoilerPictures(profile, lua.get("north"));
		east = new FPBoilerPictures(profile, lua.get("east"));
		south = new FPBoilerPictures(profile, lua.get("south"));
		west = new FPBoilerPictures(profile, lua.get("west"));
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction, int frame) {
		FPBoilerPictures dirPictures;
		if (direction == Direction.EAST) {
			dirPictures = east;
		} else if (direction == Direction.NORTH) {
			dirPictures = north;
		} else if (direction == Direction.SOUTH) {
			dirPictures = south;
		} else if (direction == Direction.WEST) {
			dirPictures = west;
		} else {
			return;
		}
		dirPictures.structure.defineSprites(consumer, frame);
	}
}