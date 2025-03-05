package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPCargoBayConnectableGraphicsSet;
import com.demod.fbsr.fp.FPCargoBayConnections;
import com.demod.fbsr.fp.FPLayeredSpriteVariations;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

//Not a real prototype, but to render cargo bay connection tilings
public abstract class CargoBayConnectionsRendering extends SimpleEntityRendering {

	private static class ElCon {
		public final ElType type;
		public final MapPosition shift;
		public final Direction direction;
		public final Function<FPCargoBayConnections, FPLayeredSpriteVariations> protoSpritesFunc;

		public ElCon(ElType type, double dx, double dy, Direction direction,
				Function<FPCargoBayConnections, FPLayeredSpriteVariations> protoSpritesFunc) {
			this.shift = MapPosition.byUnit(dx, dy);
			this.type = type;
			this.direction = direction;
			this.protoSpritesFunc = protoSpritesFunc;
		}
	}

	private static enum ElType {
		WALL(0, 0, "E|W", "N"), //
		OUTSIDE_CORNER(0, 0, "S|W", "N|E"), //
		INSIDE_CORNER(0, 0, "N|E", "NE"), //
		BRIDGE_NARROW_LEFT(0, 1, "N", "NE|W"), //
		BRIDGE_NARROW_RIGHT(0, 1, "N", "E|NW"), //
		BRIDGE_WIDE(1, 1, "NW|N", ""), //
		BRIDGE_CROSSING(-1, 1, "N|NE|E", ""),//
		;

		public final MapPosition cellOffset;
		public final List<Direction> requireOccupied;
		public final List<Direction> requireEmpty;

		private ElType(double cellDx, double cellDy, String requireOccupiedSymbols, String requireEmptySymbols) {
			cellOffset = MapPosition.byUnit(cellDx, cellDy);
			requireOccupied = Arrays.asList(requireOccupiedSymbols.split("\\|")).stream()
					.filter(s -> !s.trim().isEmpty()).map(Direction::fromSymbol).collect(Collectors.toList());
			requireEmpty = Arrays.asList(requireEmptySymbols.split("\\|")).stream().filter(s -> !s.trim().isEmpty())
					.map(Direction::fromSymbol).collect(Collectors.toList());
		}
	}

	public static long getRandomSeed(MapPosition point, ElType type, Direction direction) {
		int x = point.getXCell();
		int y = point.getYCell();
		return ((y * 73856093) ^ (x * 19349663) ^ (type.ordinal() * 83492791) ^ (direction.ordinal() * 123456789));
	}

	private FPCargoBayConnectableGraphicsSet protoGraphicsSet;

	private Optional<FPCargoBayConnectableGraphicsSet> protoPlatformGraphicsSet;
	private List<MapPosition> protoConnectionPoints;
	private List<ElCon> protoElCons;

	protected void bindCargoStationParameters(Bindings bind, LuaValue lua) {
		bindHatchDefinitions(bind, lua.get("hatch_definitions"));

		LuaValue luaGigaHatchDefinitions = lua.get("giga_hatch_definitions");
		if (!luaGigaHatchDefinitions.isnil()) {
			Utils.forEach(luaGigaHatchDefinitions.checktable(), l -> {
				LuaValue luaHatchGraphicsBack = l.get("hatch_graphics_back");
				if (!luaHatchGraphicsBack.isnil()) {
					Layer layer = FPUtils.optLayer(l.get("hatch_render_layer_back")).orElse(Layer.HIGHER_OBJECT_UNDER);
					bind.animation(luaHatchGraphicsBack).layer(layer);
				}
				LuaValue luaHatchGraphicsFront = l.get("hatch_graphics_front");
				if (!luaHatchGraphicsFront.isnil()) {
					Layer layer = FPUtils.optLayer(l.get("hatch_render_layer_front")).orElse(Layer.HIGHER_OBJECT_ABOVE);
					bind.animation(luaHatchGraphicsFront).layer(layer);
				}
			});
		}
	}

	protected void bindHatchDefinitions(Bindings bind, LuaValue lua) {
		if (!lua.isnil()) {
			Utils.forEach(lua.checktable(), l -> {
				LuaValue luaHatchGraphics = l.get("hatch_graphics");
				if (!luaHatchGraphics.isnil()) {
					Layer layer = FPUtils.optLayer(l.get("hatch_render_layer")).orElse(Layer.CARGO_HATCH);
					Optional<FPVector> offset = FPUtils.opt(l.get("offset"), FPVector::new);
					bind.animation(luaHatchGraphics).layer(layer).offset(offset.map(MapPosition::convert));
				}
			});
		}
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Consumer<LayeredSpriteDef> spriteRegister = entity.spriteRegister(register);

		FPCargoBayConnectableGraphicsSet protoSelectedGraphicsSet;

		if (map.isFoundation() && protoPlatformGraphicsSet.isPresent()) {
			protoSelectedGraphicsSet = protoPlatformGraphicsSet.get();
		} else {
			protoSelectedGraphicsSet = protoGraphicsSet;
		}

		if (protoSelectedGraphicsSet.picture.isPresent()) {
			protoSelectedGraphicsSet.picture.get().defineLayeredSprites(spriteRegister);
		}

		FPCargoBayConnections protoCargoBayConnections = protoSelectedGraphicsSet.connections.get();

		Random rand = new Random();

		elConLoop: for (ElCon elementCondition : protoElCons) {

			ElType type = elementCondition.type;
			Direction direction = elementCondition.direction;
			FPLayeredSpriteVariations protoSprites = elementCondition.protoSpritesFunc.apply(protoCargoBayConnections);

			MapPosition shift = elementCondition.shift;
			MapPosition rco = direction.rotate(type.cellOffset);
			MapPosition point = entity.getPosition().add(shift);
			MapPosition cellPoint = point.add(rco);

//			System.out.println("DEBUG ELCON ATTEMPT - " + entity.name + "#" + entity.entityNumber + " - " + type.name()
//					+ " " + direction.name() + "\n\t\t\tSHIFT " + shift + "\n\t\t\tRCO " + rco + "\n\t\t\tPOINT "
//					+ point + "\n\t\t\tCELL POINT " + cellPoint);

			for (Direction dirOccupied : type.requireOccupied) {
				Direction rotatedDirOccupied = direction.rotate(dirOccupied);
				MapPosition checkPoint = rotatedDirOccupied.offset(cellPoint, 2);
//				System.out.println("DEBUG\t\tOCCUPIED? - " + rotatedDirOccupied.name() + " - " + checkPoint + " "
//						+ map.isCargoBayConnectable(checkPoint));
				if (!map.isCargoBayConnectable(checkPoint)) {
					continue elConLoop;
				}
			}

			for (Direction dirEmpty : type.requireEmpty) {
				Direction rotatedDirEmpty = direction.rotate(dirEmpty);
				MapPosition checkPoint = rotatedDirEmpty.offset(cellPoint, 2);
//				System.out.println("DEBUG\t\tEMPTY? - " + rotatedDirEmpty.name() + " - " + checkPoint + " "
//						+ !map.isCargoBayConnectable(checkPoint));
				if (map.isCargoBayConnectable(checkPoint)) {
					continue elConLoop;
				}
			}

//			System.out.println("DEBUG\t\t\tSUCCESS!");

			rand.setSeed(getRandomSeed(point, type, direction));
			int variation = rand.nextInt(protoSprites.getVariationCount());

			Consumer<LayeredSpriteDef> pointRegister = s -> register.accept(new MapSprite(s, point));
			protoSprites.defineLayeredSprites(pointRegister, variation);
		}
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		if (protoPlatformGraphicsSet.isPresent()) {
			FPCargoBayConnectableGraphicsSet graphicsSet = protoPlatformGraphicsSet.get();
			if (graphicsSet.picture.isPresent()) {
				graphicsSet.picture.get().defineLayeredSprites(register);
			}
			graphicsSet.connections.get().getDefs(register);
		}

		if (protoGraphicsSet.picture.isPresent()) {
			protoGraphicsSet.picture.get().defineLayeredSprites(register);
		}
		protoGraphicsSet.connections.get().getDefs(register);

	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		FPBoundingBox protoSelectionBox = new FPBoundingBox(prototype.lua().get("selection_box"));

		// TODO CargoBay also has platform_graphics_set, need to figure out if needed
		protoGraphicsSet = new FPCargoBayConnectableGraphicsSet(prototype.lua().get("graphics_set"));
		protoPlatformGraphicsSet = FPUtils.opt(prototype.lua().get("platform_graphics_set"),
				FPCargoBayConnectableGraphicsSet::new);

		// XXX is there something better than selection box to determine dimensions?
		Point2D.Double p1 = protoSelectionBox.leftTop.createPoint();
		Point2D.Double p2 = protoSelectionBox.rightBottom.createPoint();

		{
			protoElCons = new ArrayList<>();

			// Outer Corners
			protoElCons.add(
					new ElCon(ElType.OUTSIDE_CORNER, p2.x - 1, p1.y + 1, Direction.NORTH, p -> p.topRightOuterCorner));
			protoElCons.add(new ElCon(ElType.OUTSIDE_CORNER, p2.x - 1, p2.y - 1, Direction.EAST,
					p -> p.bottomRightOuterCorner));
			protoElCons.add(new ElCon(ElType.OUTSIDE_CORNER, p1.x + 1, p2.y - 1, Direction.SOUTH,
					p -> p.bottomLeftOuterCorner));
			protoElCons.add(
					new ElCon(ElType.OUTSIDE_CORNER, p1.x + 1, p1.y + 1, Direction.WEST, p -> p.topLeftOuterCorner));

			// Bridge Crossings (TODO verify this is correct)
			protoElCons.add(new ElCon(ElType.BRIDGE_CROSSING, p2.x, p1.y, Direction.NORTH, p -> p.bridgeCrossing));
			protoElCons.add(new ElCon(ElType.BRIDGE_CROSSING, p2.x, p2.y, Direction.EAST, p -> p.bridgeCrossing));
			protoElCons.add(new ElCon(ElType.BRIDGE_CROSSING, p1.x, p2.y, Direction.SOUTH, p -> p.bridgeCrossing));
			protoElCons.add(new ElCon(ElType.BRIDGE_CROSSING, p1.x, p1.y, Direction.WEST, p -> p.bridgeCrossing));

			// Narrow Bridges
			protoElCons.add(
					new ElCon(ElType.BRIDGE_NARROW_LEFT, p1.x + 1, p1.y, Direction.NORTH, p -> p.bridgeVerticalNarrow));
			protoElCons.add(new ElCon(ElType.BRIDGE_NARROW_RIGHT, p2.x - 1, p1.y, Direction.NORTH,
					p -> p.bridgeVerticalNarrow));
			protoElCons.add(new ElCon(ElType.BRIDGE_NARROW_LEFT, p1.x, p2.y - 1, Direction.WEST,
					p -> p.bridgeHorizontalNarrow));
			protoElCons.add(new ElCon(ElType.BRIDGE_NARROW_RIGHT, p1.x, p1.y + 1, Direction.WEST,
					p -> p.bridgeHorizontalNarrow));

			// Walls, Inside Corners
			for (double x = p1.x + 1; x < p2.x; x += 2) {
				protoElCons.add(new ElCon(ElType.WALL, x, p1.y + 1, Direction.NORTH, p -> p.topWall));
				protoElCons
						.add(new ElCon(ElType.INSIDE_CORNER, x, p1.y + 1, Direction.NORTH, p -> p.topRightInnerCorner));
				protoElCons
						.add(new ElCon(ElType.INSIDE_CORNER, x, p1.y + 1, Direction.WEST, p -> p.topLeftInnerCorner));

				protoElCons.add(new ElCon(ElType.WALL, x, p2.y - 1, Direction.SOUTH, p -> p.bottomWall));
				protoElCons.add(
						new ElCon(ElType.INSIDE_CORNER, x, p2.y - 1, Direction.SOUTH, p -> p.bottomLeftInnerCorner));
				protoElCons.add(
						new ElCon(ElType.INSIDE_CORNER, x, p2.y - 1, Direction.EAST, p -> p.bottomRightInnerCorner));
			}
			for (double y = p1.y + 1; y < p2.y; y += 2) {
				protoElCons.add(new ElCon(ElType.WALL, p1.x + 1, y, Direction.WEST, p -> p.leftWall));
				protoElCons
						.add(new ElCon(ElType.INSIDE_CORNER, p1.x + 1, y, Direction.WEST, p -> p.topLeftInnerCorner));
				protoElCons.add(
						new ElCon(ElType.INSIDE_CORNER, p1.x + 1, y, Direction.SOUTH, p -> p.bottomLeftInnerCorner));

				protoElCons.add(new ElCon(ElType.WALL, p2.x - 1, y, Direction.EAST, p -> p.rightWall));
				protoElCons.add(
						new ElCon(ElType.INSIDE_CORNER, p2.x - 1, y, Direction.EAST, p -> p.bottomRightInnerCorner));
				protoElCons
						.add(new ElCon(ElType.INSIDE_CORNER, p2.x - 1, y, Direction.NORTH, p -> p.topRightInnerCorner));
			}

			// Wide Bridges
			for (double x = p1.x + 2; x < p2.x - 1; x += 2) {
				protoElCons.add(new ElCon(ElType.BRIDGE_WIDE, x, p1.y, Direction.NORTH, p -> p.bridgeVerticalWide));
			}
			for (double y = p1.y + 2; y < p2.y - 1; y += 2) {
				protoElCons.add(new ElCon(ElType.BRIDGE_WIDE, p1.x, y, Direction.WEST, p -> p.bridgeHorizontalWide));
			}
		}

		{
			protoConnectionPoints = new ArrayList<>();
			for (double x = p1.x + 1; x < p2.x; x += 2) {
				protoConnectionPoints.add(MapPosition.byUnit(x, p1.y + 1));
				protoConnectionPoints.add(MapPosition.byUnit(x, p2.y - 1));
			}
			for (double y = p1.y + 3; y < p2.y - 2; y += 2) {
				protoConnectionPoints.add(MapPosition.byUnit(p1.x + 1, y));
				protoConnectionPoints.add(MapPosition.byUnit(p2.x - 1, y));
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		MapPosition pos = entity.getPosition();
		for (MapPosition dcp : protoConnectionPoints) {
			map.setCargoBayConnectable(pos.add(dcp), entity);
		}
	}

}
