package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation4Way;

public class TrainStopRendering extends SimpleEntityRendering {

	private FPAnimation4Way protoRailOverlayAnimations;
	private FPAnimation4Way protoAnimations;
	private FPAnimation4Way protoTopAnimations;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		Color color;
		if (entity.color.isPresent()) {
			color = entity.color.get().createColor();
		} else {
			color = new Color(242, 0, 0, 127);
		}

		List<Sprite> topSprites = protoTopAnimations.createSprites(entity.direction, 0);
		// FIXME find a more correct way to apply tint
		topSprites.get(1).image = Utils.tintImage(topSprites.get(1).image, color);

		register.accept(RenderUtils.spriteRenderer(Layer.RAIL_BACKPLATES,
				protoRailOverlayAnimations.createSprites(entity.direction, 0), entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(protoAnimations.createSprites(entity.direction, 0), entity,
				protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, topSprites, entity, protoSelectionBox));

		if (entity.station.isPresent()) {
			String stationName = entity.station.get();
			register.accept(
					RenderUtils.drawString(Layer.OVERLAY4, entity.position.createPoint(), Color.white, stationName));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoRailOverlayAnimations = new FPAnimation4Way(prototype.lua().get("rail_overlay_animations"));
		protoAnimations = new FPAnimation4Way(prototype.lua().get("animations"));
		protoTopAnimations = new FPAnimation4Way(prototype.lua().get("top_animations"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		map.getOrCreateRailNode(dir.offset(dir.left().offset(pos, 2), 0.5)).setStation(dir);
	}
}
