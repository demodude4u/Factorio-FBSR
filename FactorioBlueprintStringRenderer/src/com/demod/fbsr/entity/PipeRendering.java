package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public class PipeRendering extends EntityWithOwnerRendering {

	public static final String[] pipeSpriteNameMapping = //
			new String[/* bits WSEN */] { //
					"straight_horizontal", // ....
					"ending_up", // ...N
					"ending_right", // ..E.
					"corner_up_right", // ..EN
					"ending_down", // .S..
					"straight_vertical", // .S.N
					"corner_down_right", // .SE.
					"t_right", // .SEN
					"ending_left", // W...
					"corner_up_left", // W..N
					"straight_horizontal", // W.E.
					"t_up", // W.EN
					"corner_down_left", // WS..
					"t_left", // WS.N
					"t_down", // WSE.
					"cross",// WSEN
			};

	private List<FPSprite> protoPipeSprites;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		int adjCode = 0;
		adjCode |= ((pipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((pipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((pipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((pipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);

		protoPipeSprites.get(adjCode).defineSprites(entity.spriteRegister(register, Layer.OBJECT));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPipeSprites.forEach(fp -> fp.defineSprites(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaPictures = prototype.lua().get("pictures");
		protoPipeSprites = Arrays.stream(pipeSpriteNameMapping).map(s -> new FPSprite(profile, luaPictures.get(s)))
				.collect(Collectors.toList());
	}

	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, MapEntity entity) {
		return map.isPipe(direction.offset(entity.getPosition()), direction.back());
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		map.setPipe(entity.getPosition());
	}
}
