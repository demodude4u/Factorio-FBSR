package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSpriteVariations;

public class HeatPipeRendering extends EntityRendererFactory<BSEntity> {

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
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		int adjCode = 0;
		adjCode |= ((heatPipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((heatPipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((heatPipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((heatPipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);

		register.accept(RenderUtils.spriteRenderer(protoConnectionSprites.get(adjCode).createSprites(data, 0), entity,
				protoSelectionBox));
	}

	public boolean heatPipeFacingMeFrom(Direction direction, WorldMap map, BSEntity entity) {
		return map.isHeatPipe(direction.offset(entity.position.createPoint()), direction.back());
	}

	@Override
	public void initFromPrototype() {
		protoConnectionSprites = Arrays.stream(heatPipeSpriteNameMapping)
				.map(s -> new FPSpriteVariations(prototype.lua().get("connection_sprites").get(s)))
				.collect(Collectors.toList());
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		map.setHeatPipe(entity.position.createPoint());
	}
}
