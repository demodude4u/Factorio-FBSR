package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapUnknownTileMarker extends MapRenderable {
	public static final Font FONT = FBSR.getGuiStyle().FONT_BP_BOLD.deriveFont(0.5f);

	private final MapPosition position;
	private final Color color;

	public MapUnknownTileMarker(MapPosition position, Color color) {
		super(Layer.ENTITY_INFO_ICON_ABOVE);
		this.position = position;
		this.color = color;
	}

	@Override
	public void render(Graphics2D g) {
		double x = position.getX();
		double y = position.getY();
		g.setColor(color);
		g.fill(new Ellipse2D.Double(x - 0.25, y - 0.25, 0.5, 0.5));
		g.setColor(Color.gray);
		g.setFont(FONT);
		g.drawString("?", (float) x - 0.125f, (float) y + 0.15f);
	}

}
