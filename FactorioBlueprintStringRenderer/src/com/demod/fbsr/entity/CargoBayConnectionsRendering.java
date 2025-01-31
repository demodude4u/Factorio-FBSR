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

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteWithLayer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPLayeredSprite;
import com.demod.fbsr.fp.FPLayeredSpriteVariations;
import com.demod.fbsr.fp.FPVector;

//Not a real prototype, but to render cargo bay connection tilings
public abstract class CargoBayConnectionsRendering extends SimpleEntityRendering<BSEntity> {

	private static class ElCon {
		public final ElType type;
		public final Point2D.Double shift;
		public final Direction direction;
		public final Function<FPCargoBayConnections, FPLayeredSpriteVariations> protoSpritesFunc;

		public ElCon(ElType type, double dx, double dy, Direction direction,
				Function<FPCargoBayConnections, FPLayeredSpriteVariations> protoSpritesFunc) {
			this.shift = new Point2D.Double(dx, dy);
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

		public final Point2D.Double cellOffset;
		public final List<Direction> requireOccupied;
		public final List<Direction> requireEmpty;

		private ElType(double cellDx, double cellDy, String requireOccupiedSymbols, String requireEmptySymbols) {
			cellOffset = new Point2D.Double(cellDx, cellDy);
			requireOccupied = Arrays.asList(requireOccupiedSymbols.split("\\|")).stream()
					.filter(s -> !s.trim().isEmpty()).map(Direction::fromSymbol).collect(Collectors.toList());
			requireEmpty = Arrays.asList(requireEmptySymbols.split("\\|")).stream().filter(s -> !s.trim().isEmpty())
					.map(Direction::fromSymbol).collect(Collectors.toList());
		}
	}

	public static class FPCargoBayConnectableGraphicsSet {
		public final Optional<FPLayeredSprite> picture;
		public final Optional<FPAnimation> animation;
		public final Optional<FPCargoBayConnections> connections;

		public FPCargoBayConnectableGraphicsSet(LuaValue lua) {
			picture = FPUtils.opt(lua.get("picture"), FPLayeredSprite::new);
			animation = FPUtils.opt(lua.get("animation"), FPAnimation::new);
			connections = FPUtils.opt(lua.get("connections"), FPCargoBayConnections::new);
		}
	}

	public static class FPCargoBayConnections {
		public final FPLayeredSpriteVariations topWall;
		public final FPLayeredSpriteVariations rightWall;
		public final FPLayeredSpriteVariations bottomWall;
		public final FPLayeredSpriteVariations leftWall;
		public final FPLayeredSpriteVariations topLeftOuterCorner;
		public final FPLayeredSpriteVariations topRightOuterCorner;
		public final FPLayeredSpriteVariations bottomLeftOuterCorner;
		public final FPLayeredSpriteVariations bottomRightOuterCorner;
		public final FPLayeredSpriteVariations topLeftInnerCorner;
		public final FPLayeredSpriteVariations topRightInnerCorner;
		public final FPLayeredSpriteVariations bottomLeftInnerCorner;
		public final FPLayeredSpriteVariations bottomRightInnerCorner;
		public final FPLayeredSpriteVariations bridgeHorizontalNarrow;
		public final FPLayeredSpriteVariations bridgeHorizontalWide;
		public final FPLayeredSpriteVariations bridgeVerticalNarrow;
		public final FPLayeredSpriteVariations bridgeVerticalWide;
		public final FPLayeredSpriteVariations bridgeCrossing;

		public FPCargoBayConnections(LuaValue lua) {
			topWall = new FPLayeredSpriteVariations(lua.get("top_wall"));
			rightWall = new FPLayeredSpriteVariations(lua.get("right_wall"));
			bottomWall = new FPLayeredSpriteVariations(lua.get("bottom_wall"));
			leftWall = new FPLayeredSpriteVariations(lua.get("left_wall"));
			topLeftOuterCorner = new FPLayeredSpriteVariations(lua.get("top_left_outer_corner"));
			topRightOuterCorner = new FPLayeredSpriteVariations(lua.get("top_right_outer_corner"));
			bottomLeftOuterCorner = new FPLayeredSpriteVariations(lua.get("bottom_left_outer_corner"));
			bottomRightOuterCorner = new FPLayeredSpriteVariations(lua.get("bottom_right_outer_corner"));
			topLeftInnerCorner = new FPLayeredSpriteVariations(lua.get("top_left_inner_corner"));
			topRightInnerCorner = new FPLayeredSpriteVariations(lua.get("top_right_inner_corner"));
			bottomLeftInnerCorner = new FPLayeredSpriteVariations(lua.get("bottom_left_inner_corner"));
			bottomRightInnerCorner = new FPLayeredSpriteVariations(lua.get("bottom_right_inner_corner"));
			bridgeHorizontalNarrow = new FPLayeredSpriteVariations(lua.get("bridge_horizontal_narrow"));
			bridgeHorizontalWide = new FPLayeredSpriteVariations(lua.get("bridge_horizontal_wide"));
			bridgeVerticalNarrow = new FPLayeredSpriteVariations(lua.get("bridge_vertical_narrow"));
			bridgeVerticalWide = new FPLayeredSpriteVariations(lua.get("bridge_vertical_wide"));
			bridgeCrossing = new FPLayeredSpriteVariations(lua.get("bridge_crossing"));
		}
	}

	public static long getRandomSeed(Point2D.Double point, ElType type, Direction direction) {
		int x = (int) Math.floor(point.x);
		int y = (int) Math.floor(point.y);
		return ((y * 73856093) ^ (x * 19349663) ^ (type.ordinal() * 83492791) ^ (direction.ordinal() * 123456789));
	}

	private FPCargoBayConnectableGraphicsSet protoGraphicsSet;

	private Optional<FPCargoBayConnectableGraphicsSet> protoPlatformGraphicsSet;
	private List<Point2D.Double> protoConnectionPoints;
	private List<ElCon> protoElCons;

	protected void bindCargoStationParameters(Bindings bind, LuaValue lua) {
		bindHatchDefinitions(bind, lua.get("hatch_definitions"));

		LuaValue luaGigaHatchDefinitions = lua.get("giga_hatch_definitions");
		if (!luaGigaHatchDefinitions.isnil()) {
			Utils.forEach(luaGigaHatchDefinitions, l -> {
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
			Utils.forEach(lua, l -> {
				LuaValue luaHatchGraphics = l.get("hatch_graphics");
				if (!luaHatchGraphics.isnil()) {
					Layer layer = FPUtils.optLayer(l.get("hatch_render_layer")).orElse(Layer.CARGO_HATCH);
					Optional<FPVector> offset = FPUtils.opt(l.get("offset"), FPVector::new);
					bind.animation(luaHatchGraphics).layer(layer).offset(offset);
				}
			});
		}
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		FPCargoBayConnections protoCargoBayConnections;
		if (map.isSpacePlatform() && protoPlatformGraphicsSet.isPresent()) {
			protoCargoBayConnections = protoPlatformGraphicsSet.get().connections.get();
		} else {
			protoCargoBayConnections = protoGraphicsSet.connections.get();
		}

		Random rand = new Random();

		elConLoop: for (ElCon elementCondition : protoElCons) {

			ElType type = elementCondition.type;
			Direction direction = elementCondition.direction;
			FPLayeredSpriteVariations protoSprites = elementCondition.protoSpritesFunc.apply(protoCargoBayConnections);

			Point2D.Double shift = elementCondition.shift;
			Point2D.Double rco = direction.rotatePoint(type.cellOffset);
			Point2D.Double point = entity.position.createPoint(shift);
			Point2D.Double cellPoint = new Point2D.Double(point.x + rco.x, point.y + rco.y);

//			System.out.println("DEBUG ELCON ATTEMPT - " + entity.name + "#" + entity.entityNumber + " - " + type.name()
//					+ " " + direction.name() + "\n\t\t\tSHIFT " + shift + "\n\t\t\tRCO " + rco + "\n\t\t\tPOINT "
//					+ point + "\n\t\t\tCELL POINT " + cellPoint);

			for (Direction dirOccupied : type.requireOccupied) {
				Direction rotatedDirOccupied = direction.rotate(dirOccupied);
				Point2D.Double checkPoint = rotatedDirOccupied.offset(cellPoint, 2);
//				System.out.println("DEBUG\t\tOCCUPIED? - " + rotatedDirOccupied.name() + " - " + checkPoint + " "
//						+ map.isCargoBayConnectable(checkPoint));
				if (!map.isCargoBayConnectable(checkPoint)) {
					continue elConLoop;
				}
			}

			for (Direction dirEmpty : type.requireEmpty) {
				Direction rotatedDirEmpty = direction.rotate(dirEmpty);
				Point2D.Double checkPoint = rotatedDirEmpty.offset(cellPoint, 2);
//				System.out.println("DEBUG\t\tEMPTY? - " + rotatedDirEmpty.name() + " - " + checkPoint + " "
//						+ !map.isCargoBayConnectable(checkPoint));
				if (map.isCargoBayConnectable(checkPoint)) {
					continue elConLoop;
				}
			}

//			System.out.println("DEBUG\t\t\tSUCCESS!");

			rand.setSeed(getRandomSeed(point, type, direction));
			int variation = rand.nextInt(protoSprites.getVariationCount());
			for (SpriteWithLayer swl : protoSprites.createSpritesWithLayers(variation)) {
				register.accept(RenderUtils.spriteRenderer(swl.getLayer(), swl.getSprite(), point,
						new FPBoundingBox(0, 0, 0, 0)));
			}

		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

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
				protoConnectionPoints.add(new Point2D.Double(x, p1.y + 1));
				protoConnectionPoints.add(new Point2D.Double(x, p2.y - 1));
			}
			for (double y = p1.y + 3; y < p2.y - 2; y += 2) {
				protoConnectionPoints.add(new Point2D.Double(p1.x + 1, y));
				protoConnectionPoints.add(new Point2D.Double(p2.x - 1, y));
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		super.populateWorldMap(map, dataTable, entity);

		BSPosition pos = entity.position;
		for (Point2D.Double dcp : protoConnectionPoints) {
			map.setCargoBayConnectable(pos.createPoint(dcp), entity);
		}
	}

}
