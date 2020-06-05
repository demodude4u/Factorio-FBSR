package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class TransportBeltRendering extends EntityRendererFactory {

	public static final String[][] transportBeltIndexName = //
			new String[/* Cardinal */][/* Bend */] { //
					{ "west_to_north_index", "north_index", "east_to_north_index" }, // North
					{ "north_to_east_index", "east_index", "south_to_east_index" }, // East
					{ "east_to_south_index", "south_index", "west_to_south_index" }, // South
					{ "south_to_west_index", "west_index", "north_to_west_index" }, // West
			};

	public static final int[][] transportBeltIndexDefaults = //
			new int[/* Cardinal */][/* Bend */] { //
					{ 6, 2, 4 }, // North
					{ 5, 0, 8 }, // East
					{ 9, 3, 11 }, // South
					{ 10, 1, 7 }, // West
			};

	// XXX I'm not using horizontal or vertical frames
	public static final int[][] transportBeltConnectorFrameMappingIndex = //
			new int[/* Cardinal */][/* Bend */] { //
					{ 6, 0, 5 }, // North
					{ 5, 0, 3 }, // East
					{ 3, 0, 4 }, // South
					{ 4, 0, 6 }, // West
			};

	public static Sprite getBeltSprite(EntityPrototype prototype, Direction direction, BeltBend bend) {
		LuaValue anim = prototype.lua().get("belt_animation_set");
		Sprite sprite = RenderUtils.getSpriteFromAnimation(anim.get("animation_set"));
		int spriteIndex;
		if (!anim.get(transportBeltIndexName[direction.cardinal()][bend.ordinal()]).isnil()) {
			spriteIndex = anim.get(transportBeltIndexName[direction.cardinal()][bend.ordinal()]).toint() - 1;
		} else {
			spriteIndex = transportBeltIndexDefaults[direction.cardinal()][bend.ordinal()];
		}
		sprite.source.y = sprite.source.height * (spriteIndex);
		return sprite;
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		BeltBend bend = map.getBeltBend(entity.getPosition()).get();

		Sprite sprite = getBeltSprite(prototype, entity.getDirection(), bend);

		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));

		JSONObject connectionsJson = entity.json().optJSONObject("connections");
		if (connectionsJson != null && connectionsJson.length() > 0) {
			int connectorFrameMappingIndex = transportBeltConnectorFrameMappingIndex[entity.getDirection()
					.cardinal()][bend.ordinal()];

			LuaValue connectorFrameSpritesLua = prototype.lua().get("connector_frame_sprites");
			Sprite connectorShadow = RenderUtils
					.getSpriteFromAnimation(connectorFrameSpritesLua.get("frame_shadow").get("sheet"));
			connectorShadow.source.y += connectorShadow.source.height * connectorFrameMappingIndex;
			Sprite connectorSprite = RenderUtils
					.getSpriteFromAnimation(connectorFrameSpritesLua.get("frame_main").get("sheet"));
			connectorSprite.source.y += connectorSprite.source.height * connectorFrameMappingIndex;

			register.accept(RenderUtils.spriteRenderer(connectorShadow, entity, prototype));
			register.accept(RenderUtils.spriteRenderer(connectorSprite, entity, prototype));
		}
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Direction dir = entity.getDirection();
		Point2D.Double pos = entity.getPosition();

		setLogisticMove(map, pos, dir.frontLeft(), dir);
		setLogisticMove(map, pos, dir.frontRight(), dir);

		BeltBend bend = map.getBeltBend(pos).get();
		switch (bend) {
		case FROM_LEFT:
			setLogisticMove(map, pos, dir.backLeft(), dir.right());
			setLogisticMove(map, pos, dir.backRight(), dir);
			break;
		case FROM_RIGHT:
			setLogisticMove(map, pos, dir.backLeft(), dir);
			setLogisticMove(map, pos, dir.backRight(), dir.left());
			break;
		case NONE:
			setLogisticMove(map, pos, dir.backLeft(), dir);
			setLogisticMove(map, pos, dir.backRight(), dir);
			break;
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setBelt(entity.getPosition(), entity.getDirection(), true, true);
	}

}
