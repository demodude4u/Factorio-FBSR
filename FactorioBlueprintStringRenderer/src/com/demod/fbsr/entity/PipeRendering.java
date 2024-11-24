package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSprite;

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

	private List<FPSprite> protoPipeSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		int adjCode = 0;
		adjCode |= ((pipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((pipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((pipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((pipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);
		register.accept(
				RenderUtils.spriteRenderer(protoPipeSprites.get(adjCode).createSprites(), entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {

		LuaValue luaPictures = prototype.lua().get("pictures");
		protoPipeSprites = Arrays.stream(pipeSpriteNameMapping).map(s -> new FPSprite(luaPictures.get(s)))
				.collect(Collectors.toList());
	}

	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, BSEntity entity) {
		return map.isPipe(direction.offset(entity.position.createPoint()), direction.back());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		map.setPipe(entity.position.createPoint());
	}

}
