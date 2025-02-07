package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.google.common.collect.ImmutableList;

public abstract class RailRendering extends EntityRendererFactory<BSEntity> {

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
		public final Optional<FPSpriteVariations> stonePathBackground;
		public final Optional<FPSpriteVariations> stonePath;
		public final Optional<FPSpriteVariations> ties;
		public final Optional<FPSpriteVariations> backplates;
		public final Optional<FPSpriteVariations> metals;

		public FPRailPieceLayers(LuaValue lua) {
			stonePathBackground = FPUtils.opt(lua.get("stone_path_background"), FPSpriteVariations::new);
			stonePath = FPUtils.opt(lua.get("stone_path"), FPSpriteVariations::new);
			ties = FPUtils.opt(lua.get("ties"), FPSpriteVariations::new);
			backplates = FPUtils.opt(lua.get("backplates"), FPSpriteVariations::new);
			metals = FPUtils.opt(lua.get("metals"), FPSpriteVariations::new);
		}
	}

	protected FPRailPictureSet protoPictures;
	private final Layer layerRailStoneBackground;
	private final Layer layerRailStone;
	private final Layer layerRailTies;
	private final Layer layerRailBackplates;
	private final Layer layerRailMetals;

	public RailRendering(boolean elevated) {
		if (elevated) {
			layerRailStoneBackground = Layer.ELEVATED_RAIL_STONE_PATH_LOWER;
			layerRailStone = Layer.ELEVATED_RAIL_STONE_PATH;
			layerRailTies = Layer.ELEVATED_RAIL_TIE;
			layerRailBackplates = Layer.ELEVATED_RAIL_SCREW;
			layerRailMetals = Layer.ELEVATED_RAIL_METAL;
		} else {
			layerRailStoneBackground = Layer.RAIL_STONE_PATH_LOWER;
			layerRailStone = Layer.RAIL_STONE_PATH;
			layerRailTies = Layer.RAIL_TIE;
			layerRailBackplates = Layer.RAIL_SCREW;
			layerRailMetals = Layer.RAIL_METAL;
		}
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {

		FPRailPieceLayers railPieceLayers = protoPictures.get(entity.direction);
		if (railPieceLayers.stonePathBackground.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(layerRailStoneBackground,
					railPieceLayers.stonePathBackground.get().createSprites(0), entity, protoSelectionBox));
		}
		if (railPieceLayers.stonePath.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(layerRailStone, railPieceLayers.stonePath.get().createSprites(0),
					entity, protoSelectionBox));
		}
		if (railPieceLayers.ties.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(layerRailTies, railPieceLayers.ties.get().createSprites(0),
					entity, protoSelectionBox));
		}
		if (railPieceLayers.backplates.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(layerRailBackplates,
					railPieceLayers.backplates.get().createSprites(0), entity, protoSelectionBox));
		}
		if (railPieceLayers.metals.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(layerRailMetals, railPieceLayers.metals.get().createSprites(0),
					entity, protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		protoPictures = new FPRailPictureSet(prototype.lua().get("pictures"));
	}

}
