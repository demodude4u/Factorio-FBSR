package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapUnknownTileMarker extends MapRenderable {
	public static final Font FONT = GUIStyle.FONT_BP_BOLD.deriveFont(0.5f);

	private final Color color;

	public MapUnknownTileMarker(MapPosition position, Color color) {
		super(Layer.ENTITY_INFO_ICON_ABOVE, position);
		this.color = color;
	}

	@Override
	public void render(Graphics2D g) {
		float x = position.getX();
		float y = position.getY();
		g.setColor(color);
		g.fill(new Ellipse2D.Float(x - 0.25f, y - 0.25f, 0.5f, 0.5f));
		g.setColor(Color.gray);
		g.setFont(FONT);
		g.drawString("?", x - 0.125f, y + 0.15f);
	}

}
