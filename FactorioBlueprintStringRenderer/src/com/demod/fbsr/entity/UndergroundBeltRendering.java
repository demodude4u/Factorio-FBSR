package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSUndergroundBeltEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.EntityType;

@EntityType("underground-belt")
public class UndergroundBeltRendering extends TransportBeltConnectableRendering {

	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;
	private FPSprite4Way protoStructureDirectionInSideLoading;
	private FPSprite4Way protoStructureDirectionOutSideLoading;
	private int protoMaxDistance;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSUndergroundBeltEntity bsEntity = entity.<BSUndergroundBeltEntity>fromBlueprint();

		defineBeltSprites(entity.spriteRegister(register, Layer.TRANSPORT_BELT), entity.getDirection().cardinal(),
				BeltBend.NONE.ordinal(), getAlternatingFrame(entity.getPosition()));

		boolean input = bsEntity.type.get().equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		FPSprite4Way protoStructSprite = input ? protoStructureDirectionIn : protoStructureDirectionOut;
		if (entity.getDirection().isHorizontal()) {
			MapPosition checkPos = entity.getPosition();
			checkPos = (input ? entity.getDirection().back() : entity.getDirection()).offset(checkPos, 0.25);
			checkPos = Direction.SOUTH.offset(checkPos, 0.75);
			boolean sideLoading = map.getLogisticGridCell(checkPos).flatMap(lgc -> lgc.getMove())
					.filter(d -> d == Direction.NORTH).isPresent();
			if (sideLoading) {
				protoStructSprite = input ? protoStructureDirectionInSideLoading
						: protoStructureDirectionOutSideLoading;
			}
		}
		protoStructSprite.defineSprites(entity.spriteRegister(register, Layer.OBJECT), structDir);
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSUndergroundBeltEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoStructureDirectionIn.getDefs(register);
		protoStructureDirectionInSideLoading.getDefs(register);
		protoStructureDirectionOut.getDefs(register);
		protoStructureDirectionOutSideLoading.getDefs(register);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(profile, luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(profile, luaStructure.get("direction_out"));
		protoStructureDirectionInSideLoading = new FPSprite4Way(profile, luaStructure.get("direction_in_side_loading"));
		protoStructureDirectionOutSideLoading = new FPSprite4Way(profile, luaStructure.get("direction_out_side_loading"));

		protoMaxDistance = prototype.lua().get("max_distance").toint();
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		BSUndergroundBeltEntity bsEntity = entity.<BSUndergroundBeltEntity>fromBlueprint();

		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();
		boolean input = bsEntity.type.get().equals("input");

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
				MapPosition targetPos = dir.offset(pos, offset);
				if (map.isMatchingUndergroundBeltEnding(entity.fromBlueprint().name, targetPos, dir)) {
					addLogisticWarp(map, pos, dir.frontLeft(), targetPos, dir.backLeft());
					addLogisticWarp(map, pos, dir.frontRight(), targetPos, dir.backRight());
					break;
				}
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		BSUndergroundBeltEntity bsEntity = entity.<BSUndergroundBeltEntity>fromBlueprint();
		boolean input = bsEntity.type.get().equals("input");

		MapPosition pos = entity.getPosition();
		if (input) {
			map.setBelt(pos, entity.getDirection(), false, false);
		} else {
			map.setBelt(pos, entity.getDirection(), false, true);
			map.setUndergroundBeltEnding(entity.fromBlueprint().name, pos, entity.getDirection());
		}
	}
}
