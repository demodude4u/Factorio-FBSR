package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Renderer;

import org.json.JSONObject;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
import com.demod.fbsr.bs.BSControlBehavior;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.TransportBeltRendering.BSTransportBeltEntity;
import com.demod.fbsr.fp.FPAnimationVariations;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class TransportBeltRendering extends TransportBeltConnectableRendering<BSTransportBeltEntity> {

	public static class BSTransportBeltEntity extends BSEntity {
		public final Optional<BSControlBehavior> controlBehavior;

		public BSTransportBeltEntity(JSONObject json) {
			super(json);

			controlBehavior = BSUtils.opt(json, "control_behavior", BSControlBehavior::new);
		}

		public BSTransportBeltEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			// TODO need to figure out what is important here
			// constructing with empty json object on purpose
			controlBehavior = BSUtils.opt(legacy.json(), "connections", j -> new BSControlBehavior(new JSONObject()));
		}
	}

	private FPAnimationVariations protoConnectorFrameMain;
	private FPAnimationVariations protoConnectorFrameShadow;

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSTransportBeltEntity entity) {
		Double pos = entity.position.createPoint();
		BeltBend bend = map.getBeltBend(pos).get();
		int frame = getAlternatingFrame(pos, 0);

		List<Sprite> beltSprites = createBeltSprites(entity.direction.cardinal(), bend.ordinal(), frame);
		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, beltSprites, entity, drawBounds));

		Point2D.Double forwardPos = entity.direction.offset(pos);
		boolean ending = true;
		Optional<BeltCell> optForwardBelt = map.getBelt(forwardPos);
		if (optForwardBelt.isPresent()) {
			Direction forwardBeltDir = optForwardBelt.get().getFacing();
			BeltBend forwardBeltBend = map.getBeltBend(forwardPos, optForwardBelt.get());
			Direction forwardBeltBackDir = forwardBeltBend.reverse(forwardBeltDir);
			if (entity.direction == forwardBeltDir.back() || entity.direction == forwardBeltBackDir.back()) {
				ending = false;
			}
		}
		if (ending) {
			List<Sprite> endingSprites = createBeltEndingSprites(entity.direction.cardinal(), frame);
			RenderUtils.shiftSprites(endingSprites, entity.direction.offset());
			register.accept(
					RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT_ENDINGS, endingSprites, entity, drawBounds));
		}

		Direction backDir = bend.reverse(entity.direction);
		Point2D.Double backPos = backDir.offset(pos);
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
			List<Sprite> startingSprites = createBeltStartingSprites(backDir.cardinal(), frame);
			RenderUtils.shiftSprites(startingSprites, backDir.offset());
			register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT_ENDINGS, startingSprites, entity,
					drawBounds));
		}

		// TODO switch this over to the wire connector logic
		if (entity.controlBehavior.isPresent()) {
			int index = transportBeltConnectorFrameMappingIndex[entity.direction.cardinal()][bend.ordinal()];
			register.accept(RenderUtils.spriteRenderer(protoConnectorFrameShadow.createSprites(data, index, 0), entity,
					drawBounds));
			register.accept(RenderUtils.spriteRenderer(protoConnectorFrameMain.createSprites(data, index, 0), entity,
					drawBounds));
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue connectorLua = prototype.lua().get("connector_frame_sprites");
		protoConnectorFrameMain = new FPAnimationVariations(connectorLua.get("frame_main"));
		protoConnectorFrameShadow = new FPAnimationVariations(connectorLua.get("frame_shadow"));
	}

	@Override
	public void populateLogistics(WorldMap map, BSTransportBeltEntity entity) {
		Direction dir = entity.direction;
		Point2D.Double pos = entity.position.createPoint();

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
	public void populateWorldMap(WorldMap map, BSTransportBeltEntity entity) {
		map.setBelt(entity.position.createPoint(), entity.direction, true, true);
	}

}
