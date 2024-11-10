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
import com.demod.fbsr.SpriteDef;
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

	public static SpriteDef[/* cardinal */][/* bend */] getBeltSprites(EntityPrototype prototype) {

		LuaValue anim = prototype.lua().get("belt_animation_set");

		SpriteDef[][] ret = new SpriteDef[4][BeltBend.values().length];

		for (int cardinal = 0; cardinal < 4; cardinal++) {
			for (int b = 0; b < BeltBend.values().length; b++) {
				SpriteDef sprite = RenderUtils.getSpriteFromAnimation(anim.get("animation_set")).get();
				int spriteIndex;
				if (!anim.get(transportBeltIndexName[cardinal][b]).isnil()) {
					spriteIndex = anim.get(transportBeltIndexName[cardinal][b]).toint() - 1;
				} else {
					spriteIndex = transportBeltIndexDefaults[cardinal][b];
				}
				// XXX Immutability violation
				sprite.getSource().y = sprite.getSource().height * (spriteIndex);
				ret[cardinal][b] = sprite;
			}
		}

		return ret;
	}

	private SpriteDef[][] protoBeltSprites;

	private SpriteDef protoConnectorShadow;

	private SpriteDef protoConnectorSprite;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		BeltBend bend = map.getBeltBend(entity.getPosition()).get();

		SpriteDef sprite = protoBeltSprites[entity.getDirection().cardinal()][bend.ordinal()];

		register.accept(RenderUtils.spriteDefRenderer(sprite, entity, protoSelectionBox));

		JSONObject connectionsJson = entity.json().optJSONObject("connections");
		if (connectionsJson != null && connectionsJson.length() > 0) {
			int connectorFrameMappingIndex = transportBeltConnectorFrameMappingIndex[entity.getDirection()
					.cardinal()][bend.ordinal()];

			Sprite connectorShadow = protoConnectorShadow.createSprite();
			connectorShadow.source.y += connectorShadow.source.height * connectorFrameMappingIndex;
			Sprite connectorSprite = protoConnectorSprite.createSprite();
			connectorSprite.source.y += connectorSprite.source.height * connectorFrameMappingIndex;

			register.accept(RenderUtils.spriteRenderer(connectorShadow, entity, protoSelectionBox));
			register.accept(RenderUtils.spriteRenderer(connectorSprite, entity, protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBeltSprites = getBeltSprites(prototype);

		LuaValue connectorFrameSpritesLua = prototype.lua().get("connector_frame_sprites");
		protoConnectorShadow = RenderUtils
				.getSpriteFromAnimation(connectorFrameSpritesLua.get("frame_shadow").get("sheet")).get();
		protoConnectorSprite = RenderUtils
				.getSpriteFromAnimation(connectorFrameSpritesLua.get("frame_main").get("sheet")).get();
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
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
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setBelt(entity.getPosition(), entity.getDirection(), true, true);
	}

}
