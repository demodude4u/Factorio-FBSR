package com.demod.fbsr.entity;

import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoint;
import com.demod.fbsr.WirePoint.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.control.BSTransportBeltControlBehavior;
import com.demod.fbsr.bs.entity.BSTransportBeltEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimationVariations;
import com.demod.fbsr.fp.FPBeltReaderLayer;
import com.demod.fbsr.fp.FPCircuitConnectorDefinition;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("transport-belt")
public class TransportBeltRendering extends TransportBeltConnectableRendering {
	private static final int CONTROL_FRAME = 0;

	private FPAnimationVariations protoConnectorFrameMain;
	private FPAnimationVariations protoConnectorFrameShadow;
	private List<FPCircuitConnectorDefinition> protoCircuitConnectors;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSTransportBeltEntity bsEntity = entity.<BSTransportBeltEntity>fromBlueprint();
		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();
		BeltCell belt = map.getBelt(pos).get();
		BeltBend bend = map.getBeltBend(pos, belt);
		int frame = getAlternatingFrame(pos);

		defineBeltSprites(entity.spriteRegister(register, Layer.TRANSPORT_BELT), dir.cardinal(),
				bend.ordinal(), frame);

		MapPosition forwardPos = dir.offset(pos);
		boolean ending = true;
		Optional<BeltCell> optForwardBelt = map.getBelt(forwardPos);
		if (optForwardBelt.isPresent()) {
			Direction forwardBeltDir = optForwardBelt.get().getFacing();
			BeltBend forwardBeltBend = map.getBeltBend(forwardPos, optForwardBelt.get());
			Direction forwardBeltBackDir = forwardBeltBend.reverse(forwardBeltDir);
			if (dir == forwardBeltDir.back() || dir == forwardBeltBackDir.back()) {
				ending = false;
			}
		}
		if (ending) {
			defineBeltEndingSprites(
					entity.spriteRegister(register, Layer.TRANSPORT_BELT_ENDINGS, dir.offset()),
					dir.cardinal(), frame);
			if (belt.isBeltReader()) {
				defineBeltReaderSprites(entity.spriteRegister(register, dir.offset()),
						beltReaderEnd[dir.cardinal()]);
			}
		}

		Direction backDir = bend.reverse(dir);
		MapPosition backPos = backDir.offset(pos);
		boolean starting = true;
		Optional<BeltCell> optBackBelt = map.getBelt(backPos);
		if (optBackBelt.isPresent()) {
			Direction backBeltDir = optBackBelt.get().getFacing();
			BeltBend backBeltBend = map.getBeltBend(backPos, optBackBelt.get());
			Direction backBeltBackDir = backBeltBend.reverse(backBeltDir);
			if (backDir == backBeltDir.back() || backDir == backBeltBackDir.back()) {
				starting = false;
			}
		}
		if (starting) {
			defineBeltStartingSprites(entity.spriteRegister(register, Layer.TRANSPORT_BELT_ENDINGS, backDir.offset()),
					backDir.cardinal(), frame);
			if (belt.isBeltReader()) {
				defineBeltReaderSprites(entity.spriteRegister(register, backDir.offset()),
						beltReaderEnd[backDir.cardinal()]);
			}
		}

		if (belt.isBeltReader()) {
			if (bend == BeltBend.NONE) { //side pieces
				{
					Direction sideDir = dir.left();
					int beltReaderframe;
					Optional<BeltCell> sideBelt = map.getBelt(sideDir.offset(pos));
					if (sideBelt.isPresent() 
							&& (sideBelt.get().getFacing() == sideDir.back())
							&& !sideBelt.get().isUndergroundInput()) {
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
					if (sideBelt.isPresent() 
							&& (sideBelt.get().getFacing() == sideDir.back())
							&& !sideBelt.get().isUndergroundInput()) {
						beltReaderframe = beltReaderBarRight[dir.cardinal()];
					} else {
						beltReaderframe = beltReaderRailRight[dir.cardinal()];
					}
					defineBeltReaderSprites(entity.spriteRegister(register), beltReaderframe);
				}

			} else { //curved piece
				defineBeltReaderSprites(entity.spriteRegister(register),
						beltReaderCurve[dir.cardinal()][bend.ordinal()]);
			}
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSTransportBeltEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoConnectorFrameMain.getDefs(register, CONTROL_FRAME);
		protoConnectorFrameShadow.getDefs(register, CONTROL_FRAME);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue connectorLua = prototype.lua().get("connector_frame_sprites");
		protoConnectorFrameMain = new FPAnimationVariations(profile, connectorLua.get("frame_main"));
		protoConnectorFrameShadow = new FPAnimationVariations(profile, connectorLua.get("frame_shadow"));

		protoCircuitConnectors = FPUtils.list(profile, prototype.lua().get("circuit_connector"), FPCircuitConnectorDefinition::new);
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();

		setLogisticMove(map, pos, dir.frontLeft(), dir);
		setLogisticMove(map, pos, dir.frontRight(), dir);

		BeltBend bend = map.getBeltBend(pos).get();
		switch (bend) {
		case FROM_LEFT:
			setLogisticMove(map, pos, dir.backLeft(), dir.right());
			setLogisticMove(map, pos, dir.backRight(), dir);
			break;
		case FROM_RIGHT:
			setLogisticMove(map, pos, dir.backLeft(), dir);
			setLogisticMove(map, pos, dir.backRight(), dir.left());
			break;
		case NONE:
			setLogisticMove(map, pos, dir.backLeft(), dir);
			setLogisticMove(map, pos, dir.backRight(), dir);
			break;
		}

		BSTransportBeltEntity bsEntity = entity.<BSTransportBeltEntity>fromBlueprint();
		boolean readAllBelts = bsEntity.controlBehavior.map(c -> c.circuitReadHandContents.orElse(false) 
				&& c.circuitContentsReadMode.orElse(0) == BSTransportBeltControlBehavior.CIRCUIT_HAND_READ_MODE_ENTIRE_BELT_HOLD).orElse(false);
		
		if (readAllBelts) {
			map.setBeltReaderSource(pos);
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		map.setBelt(new BeltCell(entity.getPosition(), entity.getDirection(), true, true, false, false) {
			@Override
			public Optional<BeltCell> nextReadAllBelts() {
				MapPosition nextPos = getFacing().offset(getPos());
				return map.getBelt(nextPos);
			}
			@Override
			public Optional<BeltCell> prevReadAllBelts() {
				Optional<BeltBend> beltBend = map.getBeltBend(getPos());
				Optional<MapPosition> prevPos = beltBend.map(b -> b.reverse(getFacing()).offset(getPos()));
				return prevPos.flatMap(map::getBelt);
			}
		});
	}

	@Override
	public void createWireConnector(Consumer<MapRenderable> register, BiConsumer<Integer, WirePoint> registerWirePoint,
			MapEntity entity, List<MapEntity> wired, WorldMap map) {
		super.createWireConnector(register, registerWirePoint, entity, wired, map);

		if (wired.isEmpty()) {
			return;
		}

		BSTransportBeltEntity bsEntity = entity.<BSTransportBeltEntity>fromBlueprint();
		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();
		Optional<BeltCell> belt = map.getBelt(pos);
		BeltBend bend = map.getBeltBend(pos, belt.get());

		int index;
		if (bend == BeltBend.NONE) {
			boolean incomingFromSide = false;
			{
				Direction sideDir = dir.left();
				Optional<BeltCell> sideBelt = map.getBelt(sideDir.offset(pos));
				if (sideBelt.isPresent() && (sideBelt.get().getFacing() == sideDir.back())) {
					incomingFromSide = true;
				}
			}
			{
				Direction sideDir = dir.right();
				Optional<BeltCell> sideBelt = map.getBelt(sideDir.offset(pos));
				if (sideBelt.isPresent() && (sideBelt.get().getFacing() == sideDir.back())) {
					incomingFromSide = true;
				}
			}
			if (incomingFromSide) {
				index = CONNECTOR_X;
			} else {
				if (dir.isHorizontal()) {
					index = CONNECTOR_H;
				} else {
					index = CONNECTOR_V;
				}
			}
		} else {
			index = connectorCurve[entity.getDirection().cardinal()][bend.ordinal()];
		}

		protoConnectorFrameShadow.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), index,
				CONTROL_FRAME);
		protoConnectorFrameMain.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), index,
				CONTROL_FRAME);

		if (protoCircuitConnectors.size() > 0) {
			FPCircuitConnectorDefinition circuitConnector = protoCircuitConnectors.get(index);
			Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);
			circuitConnector.sprites.ifPresent(sprites -> {
				sprites.connectorMain.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.connectorShadow.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.wirePins.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.wirePinsShadow.ifPresent(fp -> fp.defineSprites(entityRegister));
			});
			
			if (circuitConnector.points.isPresent()) {
				FPWireConnectionPoint cp = circuitConnector.points.get();
				registerWirePoint.accept(1, WirePoint.fromConnectionPoint(WireColor.RED, cp, entity));
				registerWirePoint.accept(2, WirePoint.fromConnectionPoint(WireColor.GREEN, cp, entity));
			}
		}
	}
}
