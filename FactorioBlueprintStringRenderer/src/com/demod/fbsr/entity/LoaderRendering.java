package com.demod.fbsr.entity;

import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSLoaderEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public abstract class LoaderRendering extends TransportBeltConnectableRendering {

	private final double beltDistance;

	private double protoContainerDistance;
	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;

	public LoaderRendering(double beltDistance) {
		this.beltDistance = beltDistance;
	}

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		BSLoaderEntity bsEntity = entity.<BSLoaderEntity>fromBlueprint();

		MapPosition beltPos = entity.getPosition().add(getBeltShift(entity));
		defineBeltSprites(s -> register.accept(new MapSprite(s, Layer.TRANSPORT_BELT, beltPos)),
				entity.getDirection().cardinal(), BeltBend.NONE.ordinal(), getAlternatingFrame(beltPos));

		boolean input = bsEntity.type.get().equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		FPSprite4Way proto = (input ? protoStructureDirectionIn : protoStructureDirectionOut);
		proto.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), structDir);

		if (!bsEntity.filters.isEmpty() && map.isAltMode()) {
			List<String> items = bsEntity.filters.stream().map(bs -> bs.name).collect(Collectors.toList());

			// TODO double/quad icons
			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Optional<BufferedImage> icon = TagManager.lookup("item", itemName);
				if (icon.isPresent()) {
					register.accept(new MapIcon(entity.getPosition(), icon.get(), 0.6, 0.1, false));
				}
			}
		}
	}

	private MapPosition getBeltShift(MapEntity entity) {
		boolean input = entity.<BSLoaderEntity>fromBlueprint().type.get().equals("input");
		Direction oppositeStructDir = input ? entity.getDirection().back() : entity.getDirection();
		return MapPosition.byUnit(beltDistance * oppositeStructDir.getDx(), beltDistance * oppositeStructDir.getDy());
	}

	private MapPosition getContainerShift(MapEntity entity, double offset) {
		boolean input = entity.<BSLoaderEntity>fromBlueprint().type.get().equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		double containerDistance = protoContainerDistance;
		containerDistance += offset;
		return MapPosition.byUnit(containerDistance * structDir.getDx(), containerDistance * structDir.getDy());
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSLoaderEntity.class;
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoContainerDistance = prototype.lua().get("container_distance").optdouble(1.5);
		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(luaStructure.get("direction_out"));
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		BSLoaderEntity bsEntity = entity.<BSLoaderEntity>fromBlueprint();

		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();
		MapPosition beltShift = getBeltShift(entity);
		MapPosition containerShift = getContainerShift(entity, -0.5);
		boolean input = bsEntity.type.get().equals("input");

		if (input) {
			MapPosition inPos = pos.add(beltShift);
			MapPosition outPos = pos.add(containerShift);

			setLogisticMoveAndAcceptFilter(map, inPos, dir.frontLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, inPos, dir.frontRight(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, inPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, inPos, dir.backRight(), dir, dir);

			if (beltShift.getX() != 0 || beltShift.getY() != 0) { // don't do this for 1x1 loaders
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontRight(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backRight(), dir, dir);
			}

			// don't sideload on the output(chest) end of the loader either
			setLogisticAcceptFilter(map, outPos, dir.back().frontLeft(), dir);
			setLogisticAcceptFilter(map, outPos, dir.back().frontRight(), dir);

			addLogisticWarp(map, outPos, dir.back().frontLeft(), outPos, dir);
			addLogisticWarp(map, outPos, dir.back().frontRight(), outPos, dir);
			map.getOrCreateLogisticGridCell(dir.back().frontLeft().offset(outPos, 0.25)).setBlockWarpFromIfMove(true);
			map.getOrCreateLogisticGridCell(dir.back().frontRight().offset(outPos, 0.25)).setBlockWarpFromIfMove(true);

		} else {
			MapPosition inPos = pos.add(containerShift);
			MapPosition outPos = pos.add(beltShift);

			setLogisticMoveAndAcceptFilter(map, outPos, dir.frontLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, outPos, dir.frontRight(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, outPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, outPos, dir.backRight(), dir, dir);

			if (beltShift.getX() != 0 || beltShift.getY() != 0) { // don't do this for 1x1 loaders
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontRight(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backRight(), dir, dir);

				// XXX really should be a filter that accepts no direction
				setLogisticMoveAndAcceptFilter(map, inPos, dir.frontLeft(), dir, dir.back());
				setLogisticMoveAndAcceptFilter(map, inPos, dir.frontRight(), dir, dir.back());
			}

			addLogisticWarp(map, inPos, dir.back(), inPos, dir.frontLeft());
			addLogisticWarp(map, inPos, dir.back(), inPos, dir.frontRight());

			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(inPos, 0.25)).setBlockWarpToIfMove(true);
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(inPos, 0.25)).setBlockWarpToIfMove(true);
		}

		if (!bsEntity.filters.isEmpty() && !input) {

			Set<String> outputs = bsEntity.filters.stream().map(bs -> bs.name)
					.collect(Collectors.toCollection(LinkedHashSet::new));

			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(pos.add(beltShift), 0.25))
					.setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(pos.add(beltShift), 0.25))
					.setOutputs(Optional.of(outputs));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		boolean input = entity.<BSLoaderEntity>fromBlueprint().type.get().equals("input");
		MapPosition beltShift = getBeltShift(entity);

		MapPosition pos = entity.getPosition();
		if (input) {
			map.setBelt(pos.add(beltShift), entity.getDirection(), false, false);
		} else {
			map.setBelt(pos.add(beltShift), entity.getDirection(), false, true);
		}
	}
}
