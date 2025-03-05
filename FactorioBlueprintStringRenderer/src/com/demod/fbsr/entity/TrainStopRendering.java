package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Renderer;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSTrainStopEntity;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public class TrainStopRendering extends SimpleEntityRendering {
	private static final int FRAME = 0;

	private FPAnimation4Way protoRailOverlayAnimations;
	private FPAnimation4Way protoAnimations;
	private FPAnimation4Way protoTopAnimations;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSTrainStopEntity bsEntity = entity.<BSTrainStopEntity>fromBlueprint();

		Color color;
		if (bsEntity.color.isPresent()) {
			color = bsEntity.color.get().createColor();
		} else {
			color = new Color(242, 0, 0, 127);
		}

		protoTopAnimations.defineSprites(null, null, FRAME);

		List<Sprite> topSprites = protoTopAnimations.createSprites(data, entity.direction, 0);
		// FIXME find a more correct way to apply tint
		topSprites.get(1).image = Utils.tintImage(topSprites.get(1).image, color);

		register.accept(RenderUtils.spriteRenderer(Layer.RAIL_SCREW,
				protoRailOverlayAnimations.createSprites(data, entity.direction, 0), entity, drawBounds));
		register.accept(RenderUtils.spriteRenderer(protoAnimations.createSprites(data, entity.direction, 0), entity,
				drawBounds));
		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, topSprites, entity, drawBounds));

		if (bsEntity.station.isPresent() && map.isAltMode()) {
			String stationName = bsEntity.station.get();
			register.accept(RenderUtils.drawString(Layer.ENTITY_INFO_TEXT, entity.position.createPoint(), Color.white,
					stationName));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoRailOverlayAnimations = new FPAnimation4Way(prototype.lua().get("rail_overlay_animations"));
		protoAnimations = new FPAnimation4Way(prototype.lua().get("animations"));
		protoTopAnimations = new FPAnimation4Way(prototype.lua().get("top_animations"));
	}

	@Override
	public void populateWorldMap(WorldMap map, BSTrainStopEntity entity) {
		super.populateWorldMap(map, entity);

		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		map.getOrCreateRailNode(dir.offset(dir.left().offset(pos, 2), 0.5)).setStation(dir);
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSTrainStopEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoRailOverlayAnimations.getDefs(register, FRAME);
		protoAnimations.getDefs(register, FRAME);
		protoTopAnimations.getDefs(register, FRAME);
	}
}
