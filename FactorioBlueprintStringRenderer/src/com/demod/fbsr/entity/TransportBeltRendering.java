package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.fp.FPAnimationVariations;

public class TransportBeltRendering extends TransportBeltConnectableRendering {

	private FPAnimationVariations protoConnectorFrameMain;
	private FPAnimationVariations protoConnectorFrameShadow;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		BeltBend bend = map.getBeltBend(entity.getPosition()).get();
		List<Sprite> beltSprites = createBeltSprites(entity.getDirection().cardinal(), bend.ordinal(), 0);
		register.accept(RenderUtils.spriteRenderer(beltSprites, entity, protoSelectionBox));

		JSONObject connectionsJson = entity.json().optJSONObject("connections");
		if (connectionsJson != null && connectionsJson.length() > 0) {
			int index = transportBeltConnectorFrameMappingIndex[entity.getDirection().cardinal()][bend.ordinal()];
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
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction dir = entity.getDirection();
		Point2D.Double pos = entity.getPosition();

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
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setBelt(entity.getPosition(), entity.getDirection(), true, true);
	}

}
