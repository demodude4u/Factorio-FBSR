package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class WallRendering extends SimpleEntityRendering {

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
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();

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

		int variation;
		if (wallSprites.getVariationCount() > 1) {
			variation = Math.abs(pos.getXCell() + pos.getYCell()) % (wallSprites.getVariationCount() / 2);
		} else {
			variation = 0;
		}
		wallSprites.defineSprites(entity.spriteRegister(register, Layer.OBJECT), variation);

		if (northGate || eastGate || southGate || westGate) {
			protoWallDiodeRed.defineSprites(entity.spriteRegister(register, Layer.OBJECT), entity.getDirection());
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.circuitConnector(lua.get("circuit_connector"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPictures.forEach(fp -> fp.getDefs(register));
		protoWallDiodeRed.getDefs().forEach(l -> l.forEach(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaPictures = prototype.lua().get("pictures");
		protoPictures = Arrays.stream(wallSpriteNameMapping).map(s -> new FPSpriteVariations(luaPictures.get(s)))
				.collect(Collectors.toList());
		protoWallDiodeRed = new FPSprite4Way(prototype.lua().get("wall_diode_red"));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		map.setWall(entity.getPosition());
	}
}
