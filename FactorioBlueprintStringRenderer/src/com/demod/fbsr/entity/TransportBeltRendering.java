package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSTransportBeltEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPAnimationVariations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class TransportBeltRendering extends TransportBeltConnectableRendering {
	private static final int CONTROL_FRAME = 0;

	private FPAnimationVariations protoConnectorFrameMain;
	private FPAnimationVariations protoConnectorFrameShadow;

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		BSTransportBeltEntity bsEntity = entity.<BSTransportBeltEntity>fromBlueprint();
		MapPosition pos = entity.getPosition();
		BeltBend bend = map.getBeltBend(pos).get();
		int frame = getAlternatingFrame(pos);

		defineBeltSprites(entity.spriteRegister(register, Layer.TRANSPORT_BELT), entity.getDirection().cardinal(),
				bend.ordinal(), frame);

		MapPosition forwardPos = entity.getDirection().offset(pos);
		boolean ending = true;
		Optional<BeltCell> optForwardBelt = map.getBelt(forwardPos);
		if (optForwardBelt.isPresent()) {
			Direction forwardBeltDir = optForwardBelt.get().getFacing();
			BeltBend forwardBeltBend = map.getBeltBend(forwardPos, optForwardBelt.get());
			Direction forwardBeltBackDir = forwardBeltBend.reverse(forwardBeltDir);
			if (entity.getDirection() == forwardBeltDir.back() || entity.getDirection() == forwardBeltBackDir.back()) {
				ending = false;
			}
		}
		if (ending) {
			defineBeltEndingSprites(
					entity.spriteRegister(register, Layer.TRANSPORT_BELT_ENDINGS, entity.getDirection().offset()),
					entity.getDirection().cardinal(), frame);
		}

		Direction backDir = bend.reverse(entity.getDirection());
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
		}

		// TODO switch this over to the wire connector logic
		if (bsEntity.controlBehavior.isPresent()) {
			int index = transportBeltConnectorFrameMappingIndex[entity.getDirection().cardinal()][bend.ordinal()];
			protoConnectorFrameShadow.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), index,
					CONTROL_FRAME);
			protoConnectorFrameMain.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), index,
					CONTROL_FRAME);
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
		protoConnectorFrameMain = new FPAnimationVariations(connectorLua.get("frame_main"));
		protoConnectorFrameShadow = new FPAnimationVariations(connectorLua.get("frame_shadow"));
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
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
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		map.setBelt(entity.getPosition(), entity.getDirection(), true, true);
	}

}
