package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;

public class WallRendering extends SimpleEntityRendering<BSEntity> {

	public static final String[] wallSpriteNameMapping = //
			new String[/* bits WSEN */] { //
					"single", // ....
					"single", // ...N
					"ending_right", // ..E.
					"ending_right", // ..EN
					"straight_vertical", // .S..
					"straight_vertical", // .S.N
					"corner_right_down", // .SE.
					"corner_right_down", // .SEN
					"ending_left", // W...
					"ending_left", // W..N
					"straight_horizontal", // W.E.
					"straight_horizontal", // W.EN
					"corner_left_down", // WS..
					"corner_left_down", // WS.N
					"t_up", // WSE.
					"t_up",// WSEN
			};
	private List<FPSpriteVariations> protoPictures;
	private FPSprite4Way protoWallDiodeRed;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		Point2D.Double pos = entity.position.createPoint();

		boolean northGate = map.isVerticalGate(Direction.NORTH.offset(pos));
		boolean eastGate = map.isHorizontalGate(Direction.EAST.offset(pos));
		boolean southGate = map.isVerticalGate(Direction.SOUTH.offset(pos));
		boolean westGate = map.isHorizontalGate(Direction.WEST.offset(pos));

		int adjCode = 0;
		adjCode |= ((map.isWall(Direction.NORTH.offset(pos)) || northGate ? 1 : 0) << 0);
		adjCode |= ((map.isWall(Direction.EAST.offset(pos)) || eastGate ? 1 : 0) << 1);
		adjCode |= ((map.isWall(Direction.SOUTH.offset(pos)) || southGate ? 1 : 0) << 2);
		adjCode |= ((map.isWall(Direction.WEST.offset(pos)) || westGate ? 1 : 0) << 3);

		FPSpriteVariations wallSprites = protoPictures.get(adjCode);
		int variation = Math.abs((int) pos.x + (int) pos.y) % (wallSprites.getVariationCount() / 2);
		register.accept(RenderUtils.spriteRenderer(wallSprites.createSprites(variation), entity, protoSelectionBox));

		if (northGate || eastGate || southGate || westGate) {
			register.accept(RenderUtils.spriteRenderer(protoWallDiodeRed.createSprites(entity.direction), entity,
					protoSelectionBox));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.circuitConnector(lua.get("circuit_connector"));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue luaPictures = prototype.lua().get("pictures");
		protoPictures = Arrays.stream(wallSpriteNameMapping).map(s -> new FPSpriteVariations(luaPictures.get(s)))
				.collect(Collectors.toList());
		protoWallDiodeRed = new FPSprite4Way(prototype.lua().get("wall_diode_red"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		map.setWall(entity.position.createPoint());
	}
}
