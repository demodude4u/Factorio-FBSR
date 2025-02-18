package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.fbsr.BoundingBoxWithHeight;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPBoundingBox;

public class ErrorRendering extends EntityRendererFactory<BSEntity> {

	private final EntityRendererFactory<? extends BSEntity> factory;

	public ErrorRendering(EntityRendererFactory<? extends BSEntity> factory) {
		this.factory = factory;
	}

	@Override
	public void createModuleIcons(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		BoundingBoxWithHeight bounds = factory.getDrawBounds().rotate(entity.direction);
		bounds = bounds.shift(pos.x, pos.y);
		register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, bounds, false) {
			@Override
			public void render(Graphics2D g) {
				g.setColor(new Color(255, 0, 0, 128));
				g.fill(new Rectangle2D.Double(bounds.x1, bounds.y1 - bounds.height, bounds.x2 - bounds.x1,
						bounds.y2 - bounds.y1 + bounds.height));
				g.setColor(Color.white);
				g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(1f));
				g.drawString("!", (float) bounds.getCenterX() - 0.25f, (float) bounds.getCenterY() + 0.3f);
			}
		});
	}

	@Override
	public Optional<WirePoint> createWirePoint(Consumer<Renderer> register, Point2D.Double position, double orientation,
			int connectionId) {
		return Optional.empty();
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

}
