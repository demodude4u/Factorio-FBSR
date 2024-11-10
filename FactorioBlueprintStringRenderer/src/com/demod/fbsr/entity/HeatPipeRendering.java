package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class HeatPipeRendering extends EntityRendererFactory {

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

	private List<SpriteDef> protoPipeSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		int adjCode = 0;
		adjCode |= ((heatPipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((heatPipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((heatPipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((heatPipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);
		SpriteDef sprite = protoPipeSprites.get(adjCode);

		register.accept(RenderUtils.spriteDefRenderer(sprite, entity, protoSelectionBox));
	}

	public boolean heatPipeFacingMeFrom(Direction direction, WorldMap map, BlueprintEntity entity) {
		return map.isHeatPipe(direction.offset(entity.getPosition()), direction.back());
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoPipeSprites = Arrays.stream(heatPipeSpriteNameMapping).map(
				s -> RenderUtils.getSpriteFromAnimation(prototype.lua().get("connection_sprites").get(s).get(1)).get())
				.collect(Collectors.toList());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setHeatPipe(entity.getPosition());
	}
}
