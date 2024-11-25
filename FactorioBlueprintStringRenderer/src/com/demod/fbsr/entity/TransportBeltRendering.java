package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimationVariations;

public class TransportBeltRendering extends TransportBeltConnectableRendering {

	private FPAnimationVariations protoConnectorFrameMain;
	private FPAnimationVariations protoConnectorFrameShadow;

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		BeltBend bend = map.getBeltBend(entity.position.createPoint()).get();
		List<Sprite> beltSprites = createBeltSprites(entity.direction.cardinal(), bend.ordinal(),
				getAlternatingFrame(entity.position.createPoint(), 0));
		register.accept(RenderUtils.spriteRenderer(beltSprites, entity, protoSelectionBox));

		// TODO switch this over to the wire connector logic
		if (entity.controlBehavior.isPresent()) {
			int index = transportBeltConnectorFrameMappingIndex[entity.direction.cardinal()][bend.ordinal()];
			register.accept(RenderUtils.spriteRenderer(protoConnectorFrameShadow.createSprites(index, 0), entity,
					protoSelectionBox));
			register.accept(RenderUtils.spriteRenderer(protoConnectorFrameMain.createSprites(index, 0), entity,
					protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue connectorLua = prototype.lua().get("connector_frame_sprites");
		protoConnectorFrameMain = new FPAnimationVariations(connectorLua.get("frame_main"));
		protoConnectorFrameShadow = new FPAnimationVariations(connectorLua.get("frame_shadow"));
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSEntity entity) {
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
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		map.setBelt(entity.position.createPoint(), entity.direction, true, true);
	}

}
