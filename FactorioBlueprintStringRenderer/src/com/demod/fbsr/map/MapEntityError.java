package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapEntityError extends MapRenderable {
	public static final Font FONT = FBSR.getGuiStyle().FONT_BP_BOLD.deriveFont(1f);

	private final MapEntity entity;

	public MapEntityError(MapEntity entity) {
		super(Layer.ENTITY_INFO_ICON_ABOVE);
		this.entity = entity;
	}

	@Override
	public void render(Graphics2D g) {
		MapRect3D bounds = entity.getBounds();
		g.setColor(new Color(255, 0, 0, 128));
		g.fill(new Rectangle2D.Double(bounds.getX1(), bounds.getY1() - bounds.getHeight(),
				bounds.getX2() - bounds.getX1(), bounds.getY2() - bounds.getY1() + bounds.getHeight()));
		g.setColor(Color.white);
		g.setFont(FONT);
		g.drawString("!", (float) bounds.getCenterX() - 0.25f, (float) bounds.getCenterY() + 0.3f);
	}

}
