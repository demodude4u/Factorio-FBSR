package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation;
import com.google.common.collect.ImmutableList;

public class BoilerRendering extends SimpleEntityRendering<BSEntity> {

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
		super.createRenderers(register, map, dataTable, entity);

		register.accept(RenderUtils.spriteRenderer(protoPictures.createSprites(entity.direction, 0), entity,
				protoSelectionBox));
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.fluidBox(lua.get("fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoPictures = new FPBoilerPictureSet(prototype.lua().get("pictures"));
		protoHasEnergySource = !prototype.lua().get("energy_source").isnil();
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		super.populateWorldMap(map, dataTable, entity);

		Direction dir = entity.direction;
		Point2D.Double position = dir.back().offset(entity.position.createPoint(), 0.5);
		if (protoHasEnergySource) {
			map.setHeatPipe(position, dir.back());// TODO in the SimpleEntityRendering
		}
	}

}
