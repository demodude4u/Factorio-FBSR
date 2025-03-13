package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapUnknownEntityMarker extends MapRenderable {
	public static final Font FONT = GUIStyle.FONT_BP_BOLD.deriveFont(0.8f);
	public static final Stroke STROKE = new BasicStroke(0.1f);

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
		g.setColor(color.darker());
		Stroke ps = g.getStroke();
		g.setStroke(STROKE);
		Ellipse2D.Double shape = new Ellipse2D.Double(x - 0.5, y - 0.5, 1, 1);
		g.fill(shape);
		g.setColor(color.brighter().brighter());
		g.draw(shape);
		g.setFont(FONT);
		g.drawString("?", (float) x - 0.175f, (float) y + 0.3f);
		g.setStroke(ps);
	}
}
