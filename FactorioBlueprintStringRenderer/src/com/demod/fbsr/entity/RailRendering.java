package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.google.common.collect.ImmutableList;

public abstract class RailRendering extends EntityRendererFactory {

	public static class FPRailPictureSet {
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
	}

	public static class FPRailPieceLayers {
		public final FPSpriteVariations stonePathBackground;
		public final FPSpriteVariations stonePath;
		public final FPSpriteVariations ties;
		public final FPSpriteVariations backplates;
		public final FPSpriteVariations metals;

		public FPRailPieceLayers(LuaValue lua) {
			stonePathBackground = new FPSpriteVariations(lua.get("stone_path_background"));
			stonePath = new FPSpriteVariations(lua.get("stone_path"));
			ties = new FPSpriteVariations(lua.get("ties"));
			backplates = new FPSpriteVariations(lua.get("backplates"));
			metals = new FPSpriteVariations(lua.get("metals"));
		}
	}

	protected FPRailPictureSet protoPictures;

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		protoPictures = new FPRailPictureSet(prototype.lua().get("pictures"));
	}

}
