package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

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

	public static final int[][][] transportBeltSpriteMapping = //
			new int[/* Cardinal */][/* Bend */][/* SXY */] { //
					{ { 8, 1, 0 }, { 1, 0, 0 }, { 8, 0, 0 } }, // North
					{ { 9, 0, 0 }, { 0, 0, 0 }, { 11, 0, 0 } }, // East
					{ { 10, 1, 0 }, { 1, 0, 1 }, { 10, 0, 0 } }, // South
					{ { 11, 1, 0 }, { 0, 1, 0 }, { 9, 1, 0 } }, // West
			};

	// XXX I'm not using horizontal or vertical frames
	public static final String[][] transportBeltConnectorFrameMapping = //
			new String[/* Cardinal */][/* Bend */] { //
					{ "nw", "x", "ne" }, // North
					{ "ne", "x", "se" }, // East
					{ "se", "x", "sw" }, // South
					{ "sw", "x", "nw" }, // West
			};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		BeltBend bend = map.getBeltBend(entity.getPosition()).get();
		int[] spriteMapping = transportBeltSpriteMapping[entity.getDirection().cardinal()][bend.ordinal()];

		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("animations"));
		sprite.source.y = sprite.source.height * spriteMapping[0];
		if (spriteMapping[1] == 1) {
			sprite.source.x += sprite.source.width;
			sprite.source.width *= -1;
		}
		if (spriteMapping[2] == 1) {
			sprite.source.y += sprite.source.height;
			sprite.source.height *= -1;
		}

		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));

		if (entity.json().has("connections")) {
			String connectorFrameMapping = transportBeltConnectorFrameMapping[entity.getDirection().cardinal()][bend
					.ordinal()];

			LuaValue connectorFrameSpritesLua = prototype.lua().get("connector_frame_sprites");
			Sprite connectorShadow = RenderUtils.getSpriteFromAnimation(
					connectorFrameSpritesLua.get("frame_shadow_" + connectorFrameMapping));
			Sprite connectorSprite = RenderUtils.getSpriteFromAnimation(
					connectorFrameSpritesLua.get("frame_main_" + connectorFrameMapping));

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
