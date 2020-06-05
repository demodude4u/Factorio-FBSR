package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class WallRendering extends EntityRendererFactory {

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

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();

		boolean northGate = map.isVerticalGate(Direction.NORTH.offset(pos));
		boolean eastGate = map.isHorizontalGate(Direction.EAST.offset(pos));
		boolean southGate = map.isVerticalGate(Direction.SOUTH.offset(pos));
		boolean westGate = map.isHorizontalGate(Direction.WEST.offset(pos));

		int adjCode = 0;
		adjCode |= ((map.isWall(Direction.NORTH.offset(pos)) || northGate ? 1 : 0) << 0);
		adjCode |= ((map.isWall(Direction.EAST.offset(pos)) || eastGate ? 1 : 0) << 1);
		adjCode |= ((map.isWall(Direction.SOUTH.offset(pos)) || southGate ? 1 : 0) << 2);
		adjCode |= ((map.isWall(Direction.WEST.offset(pos)) || westGate ? 1 : 0) << 3);
		String spriteName = wallSpriteNameMapping[adjCode];

		LuaValue spriteLua = prototype.lua().get("pictures").get(spriteName);

		List<LuaValue> layersChoices = new ArrayList<>();
		if (spriteLua.get("layers") != LuaValue.NIL) {
			layersChoices.add(spriteLua.get("layers"));
		} else {
			Utils.forEach(spriteLua, l -> layersChoices.add(l.get("layers")));
		}
		LuaValue layersLua = layersChoices.get(Math.abs((int) pos.x + (int) pos.y) % layersChoices.size());

		Sprite sprite = RenderUtils.getSpriteFromAnimation(layersLua.get(1));
		Sprite spriteShadow = RenderUtils.getSpriteFromAnimation(layersLua.get(2));

		register.accept(RenderUtils.spriteRenderer(spriteShadow, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));

		if (northGate || eastGate || southGate || westGate) {
			Sprite wallDiodeSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("wall_diode_red"));
			register.accept(RenderUtils.spriteRenderer(wallDiodeSprite, entity, prototype));
		}

	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setWall(entity.getPosition());
	}
}
