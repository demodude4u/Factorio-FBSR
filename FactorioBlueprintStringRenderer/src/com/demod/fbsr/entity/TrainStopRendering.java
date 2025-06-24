package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSTrainStopEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapText;

@EntityType("train-stop")
public class TrainStopRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	private FPColor protoColor;
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
			color = protoColor.createColor();
		}

		protoRailOverlayAnimations.defineSprites(
				entity.spriteRegisterWithTintOverride(register, Layer.RAIL_SCREW, color), entity.getDirection(), FRAME);
		protoAnimations.defineSprites(entity.spriteRegisterWithTintOverride(register, Layer.OBJECT, color),
				entity.getDirection(), FRAME);
		protoTopAnimations.defineSprites(
				entity.spriteRegisterWithTintOverride(register, Layer.HIGHER_OBJECT_UNDER, color),
				entity.getDirection(), FRAME);

		if (bsEntity.station.isPresent() && map.isAltMode()) {
			String stationName = bsEntity.station.get();
			Font FONT = profile.getGuiStyle().FONT_BP_BOLD.deriveFont(0.5f);
			register.accept(
					new MapText(Layer.ENTITY_INFO_TEXT, entity.getPosition().addUnit(0.5, -2.0), -30, FONT, Color.white, stationName, false));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.sprite4Way(lua.get("light1").get("picture")).layer(Layer.HIGHER_OBJECT_ABOVE);
		bind.sprite4Way(lua.get("light2").get("picture")).layer(Layer.HIGHER_OBJECT_ABOVE);
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

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoColor = new FPColor(prototype.lua().get("color"));
		protoRailOverlayAnimations = new FPAnimation4Way(profile, prototype.lua().get("rail_overlay_animations"));
		protoAnimations = new FPAnimation4Way(profile, prototype.lua().get("animations"));
		protoTopAnimations = new FPAnimation4Way(profile, prototype.lua().get("top_animations"));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();

		// TODO
//		map.getOrCreateRailNode(dir.offset(dir.left().offset(pos, 2), 0.5)).setStation(dir);
	}
}
