package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class UndergroundBeltRendering extends EntityRendererFactory {

	private SpriteDef[][] protoBeltSprites;
	private SpriteDef protoIn;
	private SpriteDef protoOut;
	private int protoMaxDistance;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		SpriteDef beltSprite = protoBeltSprites[entity.getDirection().cardinal()][BeltBend.NONE.ordinal()];

		Sprite sprite = (input ? protoIn : protoOut).createSprite();
		sprite.source.x += sprite.source.width * (structDir.cardinal());

		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY, beltSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, sprite, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBeltSprites = TransportBeltRendering.getBeltSprites(prototype);
		protoIn = RenderUtils.getSpriteFromAnimation(prototype.lua().get("structure").get("direction_in").get("sheet"))
				.get();
		protoOut = RenderUtils
				.getSpriteFromAnimation(prototype.lua().get("structure").get("direction_out").get("sheet")).get();

		protoMaxDistance = prototype.lua().get("max_distance").toint();
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction dir = entity.getDirection();
		Point2D.Double pos = entity.getPosition();
		boolean input = entity.json().getString("type").equals("input");

		if (input) {
			setLogisticMove(map, pos, dir.backLeft(), dir);
			setLogisticMove(map, pos, dir.backRight(), dir);
			setLogisticAcceptFilter(map, pos, dir.frontLeft(), dir);
			setLogisticAcceptFilter(map, pos, dir.frontRight(), dir);
		} else {
			// XXX really should be a filter that accepts no direction
			setLogisticMoveAndAcceptFilter(map, pos, dir.backLeft(), dir, dir.back());
			setLogisticMoveAndAcceptFilter(map, pos, dir.backRight(), dir, dir.back());
			setLogisticMove(map, pos, dir.frontLeft(), dir);
			setLogisticMove(map, pos, dir.frontRight(), dir);
		}

		if (input) {
			for (int offset = 1; offset <= protoMaxDistance; offset++) {
				Point2D.Double targetPos = dir.offset(pos, offset);
				if (map.isMatchingUndergroundBeltEnding(entity.getName(), targetPos, dir)) {
					addLogisticWarp(map, pos, dir.frontLeft(), targetPos, dir.backLeft());
					addLogisticWarp(map, pos, dir.frontRight(), targetPos, dir.backRight());
					break;
				}
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		boolean input = entity.json().getString("type").equals("input");

		if (input) {
			map.setBelt(entity.getPosition(), entity.getDirection(), false, false);
		} else {
			map.setBelt(entity.getPosition(), entity.getDirection(), false, true);
			map.setUndergroundBeltEnding(entity.getName(), entity.getPosition(), entity.getDirection());
		}
	}
}
