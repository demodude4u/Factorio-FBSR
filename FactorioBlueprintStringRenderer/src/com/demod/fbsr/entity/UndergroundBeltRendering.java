package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.UndergroundBeltRendering.BSUndergroundBeltEntity;
import com.demod.fbsr.fp.FPSprite4Way;

public class UndergroundBeltRendering extends TransportBeltConnectableRendering<BSUndergroundBeltEntity> {

	public static class BSUndergroundBeltEntity extends BSEntity {
		public final Optional<String> type;

		public BSUndergroundBeltEntity(JSONObject json) {
			super(json);

			type = BSUtils.optString(json, "type");
		}
	}

	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;
	private FPSprite4Way protoStructureDirectionInSideLoading;
	private FPSprite4Way protoStructureDirectionOutSideLoading;
	private int protoMaxDistance;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BSUndergroundBeltEntity entity) {
		List<Sprite> beltSprites = createBeltSprites(entity.direction.cardinal(), BeltBend.NONE.ordinal(),
				getAlternatingFrame(entity.position.createPoint(), 0));
		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, beltSprites, entity, protoSelectionBox));

		boolean input = entity.type.get().equals("input");
		Direction structDir = input ? entity.direction : entity.direction.back();
		FPSprite4Way protoStructSprite = input ? protoStructureDirectionIn : protoStructureDirectionOut;
		if (entity.direction.isHorizontal()) {
			Point2D.Double checkPos = entity.position.createPoint();
			checkPos = (input ? entity.direction.back() : entity.direction).offset(checkPos, 0.25);
			checkPos = Direction.SOUTH.offset(checkPos, 0.75);
			boolean sideLoading = map.getLogisticGridCell(checkPos).flatMap(lgc -> lgc.getMove())
					.filter(d -> d == Direction.NORTH).isPresent();
			if (sideLoading) {
				protoStructSprite = input ? protoStructureDirectionInSideLoading
						: protoStructureDirectionOutSideLoading;
			}
		}
		List<Sprite> structureSprites = protoStructSprite.createSprites(structDir);
		register.accept(
				RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, structureSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(luaStructure.get("direction_out"));
		protoStructureDirectionInSideLoading = new FPSprite4Way(luaStructure.get("direction_in_side_loading"));
		protoStructureDirectionOutSideLoading = new FPSprite4Way(luaStructure.get("direction_out_side_loading"));

		protoMaxDistance = prototype.lua().get("max_distance").toint();
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSUndergroundBeltEntity entity) {
		Direction dir = entity.direction;
		Point2D.Double pos = entity.position.createPoint();
		boolean input = entity.type.get().equals("input");

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
				if (map.isMatchingUndergroundBeltEnding(entity.name, targetPos, dir)) {
					addLogisticWarp(map, pos, dir.frontLeft(), targetPos, dir.backLeft());
					addLogisticWarp(map, pos, dir.frontRight(), targetPos, dir.backRight());
					break;
				}
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSUndergroundBeltEntity entity) {
		boolean input = entity.type.get().equals("input");

		Point2D.Double pos = entity.position.createPoint();
		if (input) {
			map.setBelt(pos, entity.direction, false, false);
		} else {
			map.setBelt(pos, entity.direction, false, true);
			map.setUndergroundBeltEnding(entity.name, pos, entity.direction);
		}
	}
}
