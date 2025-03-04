package com.demod.fbsr.entity;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSInserterEntity;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapInserterArm;
import com.demod.fbsr.map.MapInserterIndicators;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class InserterRendering extends SimpleEntityRendering {

	private static final int[][] placeItemDir = //
			new int[/* Cardinal */][/* Bend */] { //
					{ -1, 1, 1 }, // North
					{ 1, -1, -1 }, // East
					{ -1, -1, 1 }, // South
					{ 1, 1, -1 },// West
			};

	private FPSprite4Way protoPlatformPicture;
	private FPSprite protoHandOpenPicture;
	private FPVector protoPickupPosition;
	private FPVector protoInsertPosition;
	private FPSprite protoIndicationLine;
	private FPSprite protoIndicationArrow;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();

		BSInserterEntity bsEntity = entity.<BSInserterEntity>fromBlueprint();

		protoPlatformPicture.defineSprites(entity.spriteRegister(register, Layer.OBJECT), dir.back());

		boolean modded = bsEntity.pickupPosition.isPresent() || bsEntity.dropPosition.isPresent();

		MapPosition pickupPos;
		MapPosition insertPos;
		MapPosition inPos;
		MapPosition outPos;

		double armStretch = protoPickupPosition.y;

		if (bsEntity.pickupPosition.isPresent()) {
			pickupPos = bsEntity.pickupPosition.get().createPoint();
			inPos = pickupPos.add(pos);

		} else if (modded) {
			inPos = dir.offset(pos, -armStretch);
			pickupPos = inPos.sub(pos);

		} else {
			pickupPos = MapPosition.convert(protoPickupPosition.createPoint());
			inPos = dir.offset(pos, -armStretch);
		}

		if (bsEntity.dropPosition.isPresent()) {
			insertPos = bsEntity.dropPosition.get().createPoint();
			outPos = insertPos.add(pos);

		} else if (modded) {
			outPos = dir.offset(pos, armStretch);
			insertPos = outPos.sub(pos);

		} else {
			insertPos = MapPosition.convert(protoInsertPosition.createPoint());
			outPos = dir.offset(pos, armStretch);
		}

		register.accept(new MapInserterArm(protoHandOpenPicture.defineSprites(), pos, dir, armStretch));

		if (map.isAltMode()) {
			register.accept(new MapInserterIndicators(protoIndicationLine.defineSprites(),
					protoIndicationArrow.defineSprites(), pos, inPos, outPos, pickupPos, insertPos, dir, modded));
		}

		if (bsEntity.useFilters && map.isAltMode()) {
			List<String> items = bsEntity.filters.stream().map(bs -> bs.name).collect(Collectors.toList());

			// TODO show double/quad icons if more than one
			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Optional<BufferedImage> icon = TagManager.lookup("item", itemName);
				if (icon.isPresent()) {
					register.accept(new MapIcon(pos, icon.get(), 0.6, 0.1, false));
				}
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoPlatformPicture = new FPSprite4Way(prototype.lua().get("platform_picture"));
		protoHandOpenPicture = new FPSprite(prototype.lua().get("hand_open_picture"));

		protoPickupPosition = new FPVector(prototype.lua().get("pickup_position"));
		protoInsertPosition = new FPVector(prototype.lua().get("insert_position"));

		LuaValue optUtilityConstantsLua = data.getTable().getRaw("utility-sprites", "default").get();
		protoIndicationLine = new FPSprite(optUtilityConstantsLua.get("indication_line"));
		protoIndicationArrow = new FPSprite(optUtilityConstantsLua.get("indication_arrow"));
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		BSInserterEntity bsEntity = entity.<BSInserterEntity>fromBlueprint();

		if (bsEntity.pickupPosition.isPresent() || bsEntity.dropPosition.isPresent()) {
			return; // TODO Modded inserter logistics
		}

		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();

		MapPosition inPos = dir.offset(pos, -protoPickupPosition.y);
		MapPosition outPos = dir.offset(pos, protoPickupPosition.y);

		Direction cellDir;

		Optional<BeltCell> belt = map.getBelt(outPos);
		if (belt.isPresent()) {
			BeltBend bend = map.getBeltBend(outPos, belt.get());
			cellDir = dir.back().rotate(
					placeItemDir[belt.get().getFacing().rotate(-dir.back().ordinal()).cardinal()][bend.ordinal()]);
		} else {
			cellDir = dir.frontRight();
		}

		if (bsEntity.useFilters) {
			LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(outPos, 0.25));
			bsEntity.filters.stream().forEach(bs -> cell.addOutput(bs.name));

		} else {
			addLogisticWarp(map, inPos, dir.frontLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.frontRight(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backRight(), outPos, cellDir);
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSInserterEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPlatformPicture.getDefs().forEach(l -> l.forEach(register));
		protoHandOpenPicture.defineSprites(register);
		protoIndicationLine.defineSprites(register);
		protoIndicationArrow.defineSprites(register);
	}

}
