package com.demod.fbsr.entity;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.Layer;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSLoaderEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.map.MapEntity;
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
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();

		BSLoaderEntity bsEntity = entity.<BSLoaderEntity>fromBlueprint();

		MapPosition beltPos = entity.getPosition().add(getBeltShift(entity));
		defineBeltSprites(s -> register.accept(new MapSprite(s, Layer.TRANSPORT_BELT, beltPos)),
				entity.getDirection().cardinal(), BeltBend.NONE.ordinal(), getAlternatingFrame(beltPos));

		boolean input = bsEntity.type.orElse("input").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		FPSprite4Way proto = (input ? protoStructureDirectionIn : protoStructureDirectionOut);
		proto.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), structDir);

		if (!bsEntity.filters.isEmpty() && map.isAltMode()) {

			ModdingResolver resolver = entity.getResolver();

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
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.energySource(lua.get("energy_source"));
	}

	private MapPosition getBeltShift(MapEntity entity) {
		boolean input = entity.<BSLoaderEntity>fromBlueprint().type.orElse("input").equals("input");
		Direction oppositeStructDir = input ? entity.getDirection().back() : entity.getDirection();
		return MapPosition.byUnit(beltDistance * oppositeStructDir.getDx(), beltDistance * oppositeStructDir.getDy());
	}

	private MapPosition getContainerShift(MapEntity entity, double offset) {
		boolean input = entity.<BSLoaderEntity>fromBlueprint().type.orElse("input").equals("input");
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
		protoStructureDirectionIn = new FPSprite4Way(profile, luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(profile, luaStructure.get("direction_out"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoStructureDirectionIn.getDefs(register);
		protoStructureDirectionOut.getDefs(register);
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		BSLoaderEntity bsEntity = entity.<BSLoaderEntity>fromBlueprint();

		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();
		MapPosition beltShift = getBeltShift(entity);
		MapPosition containerShift = getContainerShift(entity, -0.5);
		boolean input = bsEntity.type.orElse("input").equals("input");

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

			Set<String> outputs = bsEntity.filters.stream().flatMap(bs -> bs.name.stream())
					.collect(Collectors.toCollection(LinkedHashSet::new));

			if (!outputs.isEmpty()) {
				map.getOrCreateLogisticGridCell(dir.frontLeft().offset(pos.add(beltShift), 0.25))
						.setOutputs(Optional.of(outputs));
				map.getOrCreateLogisticGridCell(dir.frontRight().offset(pos.add(beltShift), 0.25))
						.setOutputs(Optional.of(outputs));
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		boolean input = entity.<BSLoaderEntity>fromBlueprint().type.orElse("input").equals("input");
		MapPosition beltShift = getBeltShift(entity);

		MapPosition pos = entity.getPosition();
		if (input) {
			map.setBelt(pos.add(beltShift), entity.getDirection(), false, false, false, false);
		} else {
			map.setBelt(pos.add(beltShift), entity.getDirection(), false, true, false, false);
		}
	}
}
