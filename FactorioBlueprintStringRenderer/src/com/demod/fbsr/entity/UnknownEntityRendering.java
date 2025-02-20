package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapText;
import com.demod.fbsr.map.MapUnknownEntityMarker;

public class UnknownEntityRendering extends EntityRendererFactory<BSEntity> {
	public static final Font FONT = GUIStyle.FONT_BP_BOLD.deriveFont(0.4f);

	private final Color color;
	private final float offset;

	public UnknownEntityRendering(String name) {
		this.name = name;
		Random random = new Random(name.hashCode());
		color = RenderUtils.withAlpha(Color.getHSBColor(random.nextFloat(), 0.6f, 0.4f), 128);
		offset = random.nextFloat();
	}

	@Override
	public void createModuleIcons(Consumer<MapRenderable> register, WorldMap map, BSEntity entity) {
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, BSEntity entity) {
		MapPosition pos = entity.position.createPoint();
		register.accept(new MapUnknownEntityMarker(pos, color));
		if (map.addUnknownEntity(name)) {
			register.accept(new MapText(Layer.ENTITY_INFO_TEXT, pos.addUnit(-0.5, -0.5 + offset), null, color, name));
		}
	}

	@Override
	public Optional<WirePoint> createWirePoint(Consumer<MapRenderable> register, Point2D.Double position,
			double orientation, int connectionId) {
		return Optional.empty();
	}

	@Override
	public FactorioData getData() {
		return FactorioManager.getBaseData();
	}

	@Override
	public void initFromPrototype() {
	}

	@Override
	public void populateLogistics(WorldMap map, BSEntity entity) {
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
	}

	@Override
	public boolean isUnknown() {
		return true;
	}

}
