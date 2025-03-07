package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSSplitterEntity;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapLaneArrow;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public class LaneSplitterRendering extends TransportBeltConnectableRendering {
	private static final int STRUCTURE_FRAME = 0;
	private static final int STRUCTURE_PATCH_FRAME = 0;

	private FPAnimation4Way protoStructure;
	private Optional<FPAnimation4Way> protoStructurePatch;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		BSSplitterEntity bsEntity = entity.<BSSplitterEntity>fromBlueprint();

		MapPosition beltPos = entity.getPosition();
		Consumer<SpriteDef> beltRegister = s -> register.accept(new MapSprite(s, Layer.TRANSPORT_BELT, beltPos));
		defineBeltSprites(beltRegister, dir.cardinal(), BeltBend.NONE.ordinal(), getAlternatingFrame(beltPos));

		if (protoStructurePatch.isPresent() && (dir == Direction.WEST || dir == Direction.EAST)) {
			protoStructurePatch.get().defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), dir,
					STRUCTURE_PATCH_FRAME);
		}

		protoStructure.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), dir, STRUCTURE_FRAME);

		MapPosition pos = entity.getPosition();
		MapPosition leftPos = dir.left().offset(pos, 0.25);
		MapPosition rightPos = dir.right().offset(pos, 0.25);

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
				String itemName = bsEntity.filter.get().name;
				Optional<ImageDef> icon = TagManager.lookup("item", itemName);
				if (icon.isPresent()) {
					register.accept(new MapIcon(iconPos, icon.get(), 0.6, 0.1, false));
				}

			} else {
				register.accept(new MapLaneArrow(outputPos, dir));
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

		protoStructure.getDefs(register, STRUCTURE_FRAME);
		protoStructurePatch.ifPresent(fp -> fp.getDefs(register, STRUCTURE_FRAME));
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

		MapPosition leftPos = dir.left().offset(pos, 0.25);
		MapPosition rightPos = dir.right().offset(pos, 0.25);

		setLogisticMoveAndAcceptFilter(map, pos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, pos, dir.frontRight(), dir, dir);

		if (bsEntity.outputPriority.isPresent() && bsEntity.filter.isPresent()) {
			boolean right = bsEntity.outputPriority.get().equals("right");
			MapPosition outPos = right ? rightPos : leftPos;
			MapPosition notOutPos = !right ? rightPos : leftPos;
			String itemName = bsEntity.filter.get().name;

			map.getOrCreateLogisticGridCell(dir.offset(outPos, 0.25)).addOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.offset(notOutPos, 0.25)).addBannedOutput(itemName);

			setLogisticMoveAndAcceptFilter(map, notOutPos, dir.back(), dir, dir);

			addLogisticWarp(map, outPos, dir.back(), notOutPos, dir);

			// no sideloading
			setLogisticAcceptFilter(map, outPos, dir.back(), dir);

		} else {
			setLogisticMoveAndAcceptFilter(map, leftPos, dir.back(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, rightPos, dir.back(), dir, dir);

			addLogisticWarp(map, leftPos, dir.back(), rightPos, dir);
			addLogisticWarp(map, rightPos, dir.back(), leftPos, dir);
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
