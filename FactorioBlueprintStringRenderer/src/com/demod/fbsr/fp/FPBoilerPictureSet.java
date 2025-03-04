package com.demod.fbsr.fp;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.SpriteDef;

public class FPBoilerPictureSet {
	public final FPBoilerPictures north;
	public final FPBoilerPictures east;
	public final FPBoilerPictures south;
	public final FPBoilerPictures west;

	public FPBoilerPictureSet(LuaValue lua) {
		north = new FPBoilerPictures(lua.get("north"));
		east = new FPBoilerPictures(lua.get("east"));
		south = new FPBoilerPictures(lua.get("south"));
		west = new FPBoilerPictures(lua.get("west"));
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