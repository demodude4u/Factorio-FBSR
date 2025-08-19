package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

@EntityType("heat-pipe")
public class HeatPipeRendering extends EntityWithOwnerRendering {
	private static final int VARIATION = 0;

	public static final String[] heatPipeSpriteNameMapping = //
			new String[/* bits WSEN */] { //
					"straight_horizontal", // ....
					"ending_up", // ...N
					"ending_right", // ..E.
					"corner_right_up", // ..EN
					"ending_down", // .S..
					"straight_vertical", // .S.N
					"corner_right_down", // .SE.
					"t_right", // .SEN
					"ending_left", // W...
					"corner_left_up", // W..N
					"straight_horizontal", // W.E.
					"t_up", // W.EN
					"corner_left_down", // WS..
					"t_left", // WS.N
					"t_down", // WSE.
					"cross",// WSEN
			};

	private List<FPSpriteVariations> protoConnectionSprites;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		int adjCode = 0;
		adjCode |= ((heatPipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((heatPipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((heatPipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((heatPipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);

		protoConnectionSprites.get(adjCode).defineSprites(entity.spriteRegister(register, Layer.OBJECT), VARIATION);
	}

	public boolean heatPipeFacingMeFrom(Direction direction, WorldMap map, MapEntity entity) {
		return map.isHeatPipe(direction.offset(entity.getPosition()), direction.back());
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoConnectionSprites.forEach(fp -> fp.defineSprites(register, VARIATION));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoConnectionSprites = Arrays.stream(heatPipeSpriteNameMapping)
				.map(s -> new FPSpriteVariations(profile, prototype.lua().get("connection_sprites").get(s)))
				.collect(Collectors.toList());
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		map.setHeatPipe(entity.getPosition());
	}
}
