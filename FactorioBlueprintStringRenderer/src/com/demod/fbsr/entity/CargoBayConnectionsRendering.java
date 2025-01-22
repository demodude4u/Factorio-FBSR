package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPLayeredSpriteVariations;
import com.demod.fbsr.fp.FPVector;

//Not a real prototype, but to render cargo bay connection tilings
public abstract class CargoBayConnectionsRendering extends SimpleEntityRendering<BSEntity> {

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

	private FPCargoBayConnections protoGraphicsSetConnections;
	private List<FPVector> protoConnectionPoints;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		for (FPVector dcp : protoConnectionPoints) {
			Point2D.Double cpos = entity.position.createPoint(dcp.createPoint());

			int adjCode = 0;
			for (Direction dir : Direction.values()) {
				if (map.isCargoBayConnectable(dir.offset(cpos, 2))) {
					adjCode |= 1 << dir.ordinal();
				}
			}

			// TODO what do?
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		// TODO CargoBay also has platform_graphics_set, need to figure out if needed
		protoGraphicsSetConnections = new FPCargoBayConnections(prototype.lua().get("graphics_set").get("connections"));

		{
			protoConnectionPoints = new ArrayList<>();
			Point2D.Double p1 = protoSelectionBox.leftTop.createPoint();
			Point2D.Double p2 = protoSelectionBox.rightBottom.createPoint();
			Point2D.Double start = Direction.SOUTHEAST.offset(p1);
			Point2D.Double end = Direction.NORTHWEST.offset(p2);
			for (double x = start.x; x < p2.x; x += 2) {
				protoConnectionPoints.add(new FPVector(x, start.y));
				protoConnectionPoints.add(new FPVector(x, end.y));
			}
			for (double y = start.y + 2; y < p2.y - 2; y += 2) {
				protoConnectionPoints.add(new FPVector(start.x, y));
				protoConnectionPoints.add(new FPVector(end.x, y));
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		super.populateWorldMap(map, dataTable, entity);

		for (FPVector dcp : protoConnectionPoints) {
			map.setCargoBayConnectable(dcp.createPoint(), entity);
		}
	}

}
