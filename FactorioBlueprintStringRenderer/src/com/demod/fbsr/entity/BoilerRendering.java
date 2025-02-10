package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
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

		public List<Sprite> createSprites(FactorioData data, Direction direction, int frame) {
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
			return dirPictures.structure.createSprites(data, frame);
		}
	}

	private FPBoilerPictureSet protoPictures;
	private boolean protoHasEnergySource;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		super.createRenderers(register, map, entity);

		register.accept(RenderUtils.spriteRenderer(protoPictures.createSprites(data, entity.direction, 0), entity,
				protoSelectionBox));
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.fluidBox(lua.get("fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoPictures = new FPBoilerPictureSet(prototype.lua().get("pictures"));
		protoHasEnergySource = !prototype.lua().get("energy_source").isnil();
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		super.populateWorldMap(map, entity);

		Direction dir = entity.direction;
		Point2D.Double position = dir.back().offset(entity.position.createPoint(), 0.5);
		if (protoHasEnergySource) {
			map.setHeatPipe(position, dir.back());// TODO in the SimpleEntityRendering
		}
	}

}
