package com.demod.fbsr.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
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

		boolean input = bsEntity.type.orElse("input").equals("input");
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

		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();
		BeltCell belt = map.getBelt(pos).get();
		if (belt.isBeltReader()) {
			{
				Direction sideDir = dir.left();
				int beltReaderframe;
				Optional<BeltCell> sideBelt = map.getBelt(sideDir.offset(pos));
				if (sideBelt.isPresent() && (sideBelt.get().getFacing() == sideDir.back())) {
					beltReaderframe = beltReaderBarLeft[dir.cardinal()];
				} else {
					beltReaderframe = beltReaderRailLeft[dir.cardinal()];
				}
				defineBeltReaderSprites(entity.spriteRegister(register), beltReaderframe);
			}
			{
				Direction sideDir = dir.right();
				int beltReaderframe;
				Optional<BeltCell> sideBelt = map.getBelt(sideDir.offset(pos));
				if (sideBelt.isPresent() && (sideBelt.get().getFacing() == sideDir.back())) {
					beltReaderframe = beltReaderBarRight[dir.cardinal()];
				} else {
					beltReaderframe = beltReaderRailRight[dir.cardinal()];
				}
				defineBeltReaderSprites(entity.spriteRegister(register), beltReaderframe);
			}
		}
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
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		BSUndergroundBeltEntity bsEntity = entity.<BSUndergroundBeltEntity>fromBlueprint();
		boolean input = bsEntity.type.orElse("input").equals("input");

		MapPosition pos = entity.getPosition();
		map.setBelt(new UndergroundBeltCell(map, pos, entity.getDirection(), input));
		if (!input) {
			map.setUndergroundBeltEnding(entity.fromBlueprint().name, pos, entity.getDirection());
		}
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		BSUndergroundBeltEntity bsEntity = entity.<BSUndergroundBeltEntity>fromBlueprint();

		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();
		boolean input = bsEntity.type.orElse("input").equals("input");

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
					map.linkUndergroundBelts(pos, targetPos);
					break;
				}
			}
		}
	}

	private static class UndergroundBeltCell extends BeltCell {
		private final WorldMap map;
		private final boolean input;

		public UndergroundBeltCell(WorldMap map, MapPosition pos, Direction facing, boolean input) {
			super(pos, facing, false, !input, input, !input);
			this.map = map;
			this.input = input;
		}

		@Override
		public Optional<BeltCell> nextReadAllBelts() {
			if (input) {
				Optional<MapPosition> linked = map.getLinkedUndergroundBelt(pos);
				if (!linked.isPresent()) {
					return Optional.empty();
				}
				return map.getBelt(linked.get());
			} else {
				MapPosition nextPos = facing.offset(pos);
				return map.getBelt(nextPos);
			}
		}

		@Override
		public Optional<BeltCell> prevReadAllBelts() {
			if (!input) {
				Optional<MapPosition> linked = map.getLinkedUndergroundBelt(pos);
				if (!linked.isPresent()) {
					return Optional.empty();
				}
				return map.getBelt(linked.get());
			} else {
				MapPosition nextPos = facing.back().offset(pos);
				return map.getBelt(nextPos);
			}
		}
	}
}
