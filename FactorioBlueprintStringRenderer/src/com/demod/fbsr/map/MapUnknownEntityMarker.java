package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapUnknownEntityMarker extends MapRenderable {
	public static final Font FONT = GUIStyle.FONT_BP_BOLD.deriveFont(1f);

	private final MapPosition position;
	private final Color color;

	public MapUnknownEntityMarker(MapPosition position, Color color) {
		super(Layer.ENTITY_INFO_ICON_ABOVE);
		this.position = position;
		this.color = color;
	}

	@Override
	public void render(Graphics2D g) {
		double x = position.getX();
		double y = position.getY();
		g.setColor(color);
		g.fill(new Ellipse2D.Double(x - 0.5, y - 0.5, 1, 1));
		g.setColor(Color.gray);
		g.setFont(FONT);
		g.drawString("?", (float) x - 0.25f, (float) y + 0.3f);
	}

}
