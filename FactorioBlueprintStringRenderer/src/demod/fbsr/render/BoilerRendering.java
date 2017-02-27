package demod.fbsr.render;

import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.BlueprintEntity.Direction;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;

public class BoilerRendering extends TypeRendererFactory {

	public static final String[] pipeSpriteNameMapping = //
			new String[/* bits WSEN */] { //
					"down", // ....
					"down", // ...N
					"left", // ..E.
					"right_up", // ..EN
					"down", // .S..
					"down", // .S.N
					"right_down", // .SE.
					"right_down", // .SEN
					"left", // W...
					"left_up", // W..N
					"left", // W.E.
					"t_up", // W.EN
					"left_down", // WS..
					"t_down", // WS.N
					"t_down", // WSE.
					"t_down",// WSEN
			};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		int adjCode = 0;
		adjCode |= ((pipeFacingMeFrom(Direction.NORTH, map, entity) ? 1 : 0) << 0);
		adjCode |= ((pipeFacingMeFrom(Direction.EAST, map, entity) ? 1 : 0) << 1);
		adjCode |= ((pipeFacingMeFrom(Direction.SOUTH, map, entity) ? 1 : 0) << 2);
		adjCode |= ((pipeFacingMeFrom(Direction.WEST, map, entity) ? 1 : 0) << 3);
		String spriteName = pipeSpriteNameMapping[adjCode];

		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("structure").get(spriteName));

		register.accept(spriteRenderer(sprite, entity, prototype));
	}

	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, BlueprintEntity entity) {
		return map.isPipe(direction.offset(entity.getPosition()), direction.back());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		map.setPipe(entity.getPosition());
	}

}
