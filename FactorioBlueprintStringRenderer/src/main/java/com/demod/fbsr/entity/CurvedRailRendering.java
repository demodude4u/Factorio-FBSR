package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class CurvedRailRendering extends EntityRendererFactory {
	private static final String[] railNames = { //
			"curved_rail_vertical_left_bottom", //
			"curved_rail_vertical_right_bottom", //
			"curved_rail_horizontal_left_top", //
			"curved_rail_horizontal_left_bottom", //
			"curved_rail_vertical_right_top", //
			"curved_rail_vertical_left_top", //
			"curved_rail_horizontal_right_bottom", //
			"curved_rail_horizontal_right_top", //
	};

	private static final int[][][] pathEnds = //
			new int[/* dir */][/* points */][/* x,y,dir */] { //
					{ { 1, 4, 0 }, { -2, -3, 3 } }, // N
					{ { -1, 4, 0 }, { 2, -3, 5 } }, // NE
					{ { -4, 1, 2 }, { 3, -2, 5 } }, // E
					{ { -4, -1, 2 }, { 3, 2, 7 } }, // SE
					{ { -1, -4, 4 }, { 2, 3, 7 } }, // S
					{ { 1, -4, 4 }, { -2, 3, 1 } }, // SW
					{ { 4, -1, 6 }, { -3, 2, 1 } }, // W
					{ { 4, 1, 6 }, { -3, -2, 3 } }, // NW
			};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		String railName = railNames[entity.getDirection().ordinal()];
		LuaValue pictureRailLua = prototype.lua().get("pictures").get(railName);
		for (Entry<String, Layer> entry : StraightRailRendering.railLayers.entrySet()) {
			Sprite railLayerSprite = RenderUtils.getSpriteFromAnimation(pictureRailLua.get(entry.getKey()));
			register.accept(RenderUtils.spriteRenderer(entry.getValue(), railLayerSprite, entity, prototype));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		int[][] points = pathEnds[dir.ordinal()];
		Point2D.Double p1 = new Point2D.Double(pos.x + points[0][0], pos.y + points[0][1]);
		Direction d1 = Direction.values()[points[0][2]];
		Point2D.Double p2 = new Point2D.Double(pos.x + points[1][0], pos.y + points[1][1]);
		Direction d2 = Direction.values()[points[1][2]];
		Point2D.Double cp1 = d1.offset(p1, 0.5);
		Point2D.Double cp2 = d2.offset(p2, 0.5);

		map.setRailEdge(p1, d1, cp1, d1.back(), false);
		map.setRailEdge(cp1, d1, cp2, d2, true);
		map.setRailEdge(cp2, d2.back(), p2, d2, false);
	}
}
