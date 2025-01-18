package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
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
	public void createModuleIcons(Consumer<Renderer> register, WorldMap map, DataTable table, BSEntity entity) {
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		FPBoundingBox box = factory.getBounds();
		Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x + box.leftTop.x, pos.y + box.leftTop.y,
				box.rightBottom.x - box.leftTop.x, box.rightBottom.y - box.leftTop.y);
		register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, bounds, false) {
			@Override
			public void render(Graphics2D g) {
				g.setColor(Color.red);
				g.fill(bounds);
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
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSEntity entity) {
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
	}

}
