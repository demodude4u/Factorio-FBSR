package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation;
import com.google.common.collect.ImmutableList;

public class BoilerRendering extends EntityRendererFactory<BSEntity> {

	public static class FPBoilerPictures {
		public final FPAnimation structure;

		public FPBoilerPictures(LuaValue lua) {
			structure = new FPAnimation(lua.get("structure"));
		}
	}

	public static class FPBoilerPictureSet {
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

		public List<Sprite> createSprites(Direction direction, int frame) {
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
				return ImmutableList.of();
			}
			return dirPictures.structure.createSprites(frame);
		}
	}

	private FPBoilerPictureSet protoPictures;
	private boolean protoHasEnergySource;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		register.accept(RenderUtils.spriteRenderer(protoPictures.createSprites(entity.direction, 0), entity,
				protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {

		protoPictures = new FPBoilerPictureSet(prototype.lua().get("pictures"));
		protoHasEnergySource = !prototype.lua().get("energy_source").isnil();
	}

//	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, BSEntity entity) {
//		return map.isPipe(direction.offset(entity.getPosition()), direction.back());
//	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		Direction dir = entity.direction;
		Point2D.Double position = dir.back().offset(entity.position.createPoint(), 0.5);
		map.setPipe(dir.offset(position, 1), dir);
		map.setPipe(dir.left().offset(position, 1), dir.left());
		map.setPipe(dir.right().offset(position, 1), dir.right());

		if (protoHasEnergySource) {
			map.setHeatPipe(position, dir.back());
		}
	}

}
