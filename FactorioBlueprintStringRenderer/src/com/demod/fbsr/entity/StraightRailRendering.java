package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class StraightRailRendering extends EntityRendererFactory {

	private static final int[][][] pathEnds = //
			new int[/* dir */][/* points */][/* x,y,dir */] { //
					{ { 0, -1, 4 }, { 0, 1, 0 } }, // N
					{ { 0, -1, 3 }, { 1, 0, 7 } }, // NE
					{ { -1, 0, 2 }, { 1, 0, 6 } }, // E
					{ { 0, 1, 1 }, { 1, 0, 5 } }, // SE
					{ { 0, -1, 4 }, { 0, 1, 0 } }, // S
					{ { -1, 0, 3 }, { 0, 1, 7 } }, // SW
					{ { -1, 0, 2 }, { 1, 0, 6 } }, // W
					{ { -1, 0, 1 }, { 0, -1, 5 } }, // NW
			};

	public static final LinkedHashMap<String, Layer> railLayers = new LinkedHashMap<>();

	static {
		railLayers.put("stone_path_background", Layer.RAIL_STONE_BACKGROUND);
		railLayers.put("stone_path", Layer.RAIL_STONE);
		railLayers.put("ties", Layer.RAIL_TIES);
		railLayers.put("backplates", Layer.RAIL_BACKPLATES);
		railLayers.put("metals", Layer.RAIL_METALS);
	}
	private ArrayList<LinkedHashMap<Layer, SpriteDef>> protoDirRailLayers;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		LinkedHashMap<Layer, SpriteDef> railLayers = protoDirRailLayers.get(entity.getDirection().ordinal());
		for (Entry<Layer, SpriteDef> entry : railLayers.entrySet()) {
			register.accept(RenderUtils.spriteDefRenderer(entry.getKey(), entry.getValue(), entity, protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoDirRailLayers = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			if (direction.ordinal() > 3) {
				direction = direction.back();
			}
			LuaValue pictureRailLua = prototype.lua().get("pictures").get(direction.name().toLowerCase());
			LinkedHashMap<Layer, SpriteDef> layers = new LinkedHashMap<>();
			for (Entry<String, Layer> entry : StraightRailRendering.railLayers.entrySet()) {
				layers.put(entry.getValue(),
						RenderUtils.getSpriteFromAnimation(pictureRailLua.get(entry.getKey())).get());
			}
			protoDirRailLayers.add(layers);
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		int[][] points = pathEnds[dir.ordinal()];
		Point2D.Double p1 = new Point2D.Double(pos.x + points[0][0], pos.y + points[0][1]);
		Direction d1 = Direction.values()[points[0][2]];
		Point2D.Double p2 = new Point2D.Double(pos.x + points[1][0], pos.y + points[1][1]);
		Direction d2 = Direction.values()[points[1][2]];

		Point2D.Double cp1 = d1.offset(p1, 0.5);
		Point2D.Double cp2 = d2.offset(p2, 0.5);

		if (dir.isCardinal()) {
			map.setRailEdge(p1, d1, cp1, d2, false);
			map.setRailEdge(cp1, d1, cp2, d2, false);
			map.setRailEdge(cp2, d1, p2, d2, false);
		} else {
			map.setRailEdge(p1, d1, cp1, d2, false);
			map.setRailEdge(cp1, d1, p2, d2, false);
		}

	}
}
