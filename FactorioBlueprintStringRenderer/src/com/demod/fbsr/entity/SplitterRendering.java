package com.demod.fbsr.entity;

import java.awt.geom.Path2D;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.bs.entity.BSSplitterEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.map.MapBeltArrow;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapLaneArrow;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public class SplitterRendering extends TransportBeltConnectableRendering {
	private static final int STRUCTURE_FRAME = 0;
	private static final int STRUCTURE_PATCH_FRAME = 0;

	private static final Path2D.Double markerShape = new Path2D.Double();
	static {
		markerShape.moveTo(-0.5 + 0.2, 0.5 - 0.125);
		markerShape.lineTo(0.5 - 0.2, 0.5 - 0.125);
		markerShape.lineTo(0, 0 + 0.125);
		markerShape.closePath();
	}

	private FPAnimation4Way protoStructure;
	private Optional<FPAnimation4Way> protoStructurePatch;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		BSSplitterEntity bsEntity = entity.<BSSplitterEntity>fromBlueprint();

		MapPosition belt1Pos = dir.left().offset(entity.getPosition(), 0.5);
		MapPosition belt2Pos = dir.right().offset(entity.getPosition(), 0.5);
		Consumer<SpriteDef> belt1Register = s -> register.accept(new MapSprite(s, Layer.TRANSPORT_BELT, belt1Pos));
		Consumer<SpriteDef> belt2Register = s -> register.accept(new MapSprite(s, Layer.TRANSPORT_BELT, belt2Pos));
		defineBeltSprites(belt1Register, dir.cardinal(), BeltBend.NONE.ordinal(), getAlternatingFrame(belt1Pos));
		defineBeltSprites(belt2Register, dir.cardinal(), BeltBend.NONE.ordinal(), getAlternatingFrame(belt2Pos));

		if (protoStructurePatch.isPresent() && (dir == Direction.WEST || dir == Direction.EAST)) {
			protoStructurePatch.get().defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), dir,
					STRUCTURE_PATCH_FRAME);
		}

		protoStructure.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), dir, STRUCTURE_FRAME);

		MapPosition pos = entity.getPosition();
		MapPosition leftPos = dir.left().offset(pos, 0.5);
		MapPosition rightPos = dir.right().offset(pos, 0.5);

		if (bsEntity.inputPriority.isPresent() && map.isAltMode()) {
			boolean right = bsEntity.inputPriority.get().equals("right");
			MapPosition inputPos = dir.offset(right ? rightPos : leftPos, 0);

			register.accept(new MapLaneArrow(inputPos, dir));
		}

		if (bsEntity.outputPriority.isPresent() && map.isAltMode()) {
			boolean right = bsEntity.outputPriority.get().equals("right");
			MapPosition outputPos = dir.offset(right ? rightPos : leftPos, 0.6);

			if (bsEntity.filter.isPresent()) {
				MapPosition iconPos = right ? rightPos : leftPos;
				BSFilter filter = bsEntity.filter.get();
				IconManager.lookupFilter(filter.type, filter.name, filter.quality)
						.ifPresent(i -> register.accept(i.createMapIcon(iconPos, 0.5, OptionalDouble.of(0.1), false)));
			} else {
				register.accept(new MapBeltArrow(outputPos, dir));
			}
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSSplitterEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		if (protoStructurePatch.isPresent()) {
			protoStructurePatch.get().getDefs(register, STRUCTURE_PATCH_FRAME);
		}
		protoStructure.getDefs(register, STRUCTURE_FRAME);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoStructurePatch = FPUtils.opt(prototype.lua().get("structure_patch"), FPAnimation4Way::new);
		protoStructure = new FPAnimation4Way(prototype.lua().get("structure"));
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();
		BSSplitterEntity bsEntity = entity.<BSSplitterEntity>fromBlueprint();

		MapPosition leftPos = dir.left().offset(pos, 0.5);
		MapPosition rightPos = dir.right().offset(pos, 0.5);

		setLogisticMoveAndAcceptFilter(map, leftPos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, leftPos, dir.frontRight(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, rightPos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, rightPos, dir.frontRight(), dir, dir);

		if (bsEntity.outputPriority.isPresent() && bsEntity.filter.flatMap(f -> f.name).isPresent()) {
			boolean right = bsEntity.outputPriority.get().equals("right");
			MapPosition outPos = right ? rightPos : leftPos;
			MapPosition notOutPos = !right ? rightPos : leftPos;
			String itemName = bsEntity.filter.get().name.get();

			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(outPos, 0.25)).addOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(outPos, 0.25)).addOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(notOutPos, 0.25)).addBannedOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(notOutPos, 0.25)).addBannedOutput(itemName);

			setLogisticMoveAndAcceptFilter(map, notOutPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, notOutPos, dir.backRight(), dir, dir);

			addLogisticWarp(map, outPos, dir.backLeft(), notOutPos, dir.frontLeft());
			addLogisticWarp(map, outPos, dir.backRight(), notOutPos, dir.frontRight());

			// no sideloading
			setLogisticAcceptFilter(map, outPos, dir.backLeft(), dir);
			setLogisticAcceptFilter(map, outPos, dir.backRight(), dir);

		} else {
			setLogisticMoveAndAcceptFilter(map, leftPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, leftPos, dir.backRight(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, rightPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, rightPos, dir.backRight(), dir, dir);

			addLogisticWarp(map, leftPos, dir.backLeft(), rightPos, dir.frontLeft());
			addLogisticWarp(map, leftPos, dir.backRight(), rightPos, dir.frontRight());
			addLogisticWarp(map, rightPos, dir.backLeft(), leftPos, dir.frontLeft());
			addLogisticWarp(map, rightPos, dir.backRight(), leftPos, dir.frontRight());
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		Direction direction = entity.getDirection();
		MapPosition pos = entity.getPosition();
		MapPosition belt1Pos = direction.left().offset(pos, 0.5);
		MapPosition belt2Pos = direction.right().offset(pos, 0.5);
		map.setBelt(belt1Pos, direction, false, true);
		map.setBelt(belt2Pos, direction, false, true);
	}
}
