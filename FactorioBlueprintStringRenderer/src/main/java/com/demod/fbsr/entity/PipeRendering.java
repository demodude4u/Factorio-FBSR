package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class PipeRendering extends EntityRendererFactory {

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

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		int adjCode = 0;
		adjCode |= ((pipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((pipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((pipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((pipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);
		String spriteName = pipeSpriteNameMapping[adjCode];

		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("pictures").get(spriteName));

		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
	}

	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, BlueprintEntity entity) {
		return map.isPipe(direction.offset(entity.getPosition()), direction.back());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setPipe(entity.getPosition());
	}

}
