package com.demod.fbsr.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.DataTable;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSInserterEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPUtilitySprites;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapInserterArm;
import com.demod.fbsr.map.MapInserterIndicators;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("inserter")
public class InserterRendering extends EntityWithOwnerRendering {

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

	private FPSprite protoBlacklist;

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
			boolean blacklist = bsEntity.filterMode.map(s -> s.equals("blacklist")).orElse(false);

			ModdingResolver resolver = entity.getResolver();

			if (!bsEntity.filters.isEmpty()) {

				List<IconDefWithQuality> icons = bsEntity.filters.stream()
						.flatMap(f -> resolver.resolveFilter(f.type, f.name, f.quality).stream())
						.sorted(Comparator.comparing(iwq -> iwq.getDef().getPrototype())).limit(4)
						.collect(Collectors.toList());

				MapPosition iconStartPos;
				if (icons.size() == 2) {
					iconStartPos = pos.addUnit(-0.25, 0);
				} else if (icons.size() > 2) {
					iconStartPos = pos.addUnit(-0.25, -0.25);
				} else {
					iconStartPos = pos;
				}

				boolean iconBig = icons.size() == 1;
				double iconShift = 0.5;
				double iconSize = iconBig ? 0.5 : 0.4;
				double iconBorder = iconBig ? 0.1 : 0.05;

				for (int i = 0; i < icons.size(); i++) {
					IconDefWithQuality icon = icons.get(i);
					MapPosition iconPos = iconStartPos.addUnit((i % 2) * iconShift, (i / 2) * iconShift);
					register.accept(icon.createMapIcon(iconPos, iconSize, OptionalDouble.of(iconBorder), false, resolver));
					if (blacklist) {
						register.accept(new MapIcon(iconPos, protoBlacklist.defineSprites().get(0), iconSize,
								OptionalDouble.empty(), false, Optional.empty(), resolver));
					}
				}

			} else if (blacklist) {
				register.accept(new MapIcon(pos, protoBlacklist.defineSprites().get(0), 0.5, OptionalDouble.of(0.1),
						false, Optional.empty(), resolver));
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.energySource(lua.get("energy_source"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSInserterEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPlatformPicture.getDefs(register);

		Consumer<? super SpriteDef> registerNoTrim = def -> {
			def.setTrimmable(false);
			register.accept(def);
		};

		protoHandOpenPicture.defineSprites(registerNoTrim);
		protoIndicationLine.defineSprites(registerNoTrim);
		protoIndicationArrow.defineSprites(registerNoTrim);

		protoBlacklist.defineSprites(registerNoTrim);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoPlatformPicture = new FPSprite4Way(profile, prototype.lua().get("platform_picture"));
		protoHandOpenPicture = new FPSprite(profile, prototype.lua().get("hand_open_picture"));

		protoPickupPosition = new FPVector(prototype.lua().get("pickup_position"));
		protoInsertPosition = new FPVector(prototype.lua().get("insert_position"));

		FactorioManager factorioManager = profile.getFactorioManager();
		FPUtilitySprites utilitySprites;
		if (factorioManager != null) {
			utilitySprites = factorioManager.getUtilitySprites();
		} else {
			DataTable baseTable = prototype.getTable();
			utilitySprites = new FPUtilitySprites(profile, baseTable.getRaw("utility-sprites", "default").get());
		}

		protoIndicationLine = utilitySprites.indicationLine;
		protoIndicationArrow = utilitySprites.indicationArrow;

		protoBlacklist = utilitySprites.filterBlacklist;
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);
		
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
			bsEntity.filters.stream().forEach(bs -> {
				if (bs.name.isPresent()) {
					cell.addOutput(bs.name.get());
				}
			});

		} else {
			addLogisticWarp(map, inPos, dir.frontLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.frontRight(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backRight(), outPos, cellDir);
		}
	}

}
