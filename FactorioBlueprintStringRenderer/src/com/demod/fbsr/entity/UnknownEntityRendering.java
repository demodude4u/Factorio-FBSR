package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Font;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Profile;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapText;
import com.demod.fbsr.map.MapUnknownEntityMarker;

public class UnknownEntityRendering extends EntityRendererFactory {
	private final Color color;
	private final Color secondaryColor;
	private final float offset;

	private Profile profile;

	public UnknownEntityRendering(Profile profile, String name) {
		this.profile = profile;
		this.name = name;
		color = RenderUtils.getUnknownColor(name);
		secondaryColor = RenderUtils.getUnknownSecondaryColor(name);
		offset = RenderUtils.getUnknownTextOffset(name);

		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("type", "unknown");
		setPrototype(new EntityPrototype(new LuaTable(json)));

		unknownModded = true;
	}

	@Override
	public void createModuleIcons(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		MapPosition pos = entity.getPosition();
		register.accept(new MapUnknownEntityMarker(pos, color, secondaryColor));
		if (map.addUnknownEntity(name)) {
			Font FONT = profile.getGuiStyle().FONT_BP_BOLD.deriveFont(0.4f);
			register.accept(
					new MapText(Layer.ENTITY_INFO_TEXT, pos.addUnit(-0.5, -0.5 + offset), 0, FONT, Color.white, name, false, entity.getResolver()));
		}
	}

	@Override
	public Optional<WirePoint> createWirePoint(Consumer<MapRenderable> register, MapPosition position,
			double orientation, int connectionId) {
		return Optional.empty();
	}

	@Override
	public Profile getProfile() {
		return profile;
	}

	@Override
	public MapRect3D getDrawBounds(MapEntity entity) {
		return MapRect3D.byUnit(-0.5, -0.5, 0.5, 0.5, 0).shift(entity.getPosition());
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
	}

	@Override
	public void initFromPrototype() {
	}

	@Override
	public boolean isUnknown() {
		return true;
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
	}

	@Override
	public boolean isEntityTypeMatch(EntityPrototype proto) {
		return true;
	}
}
