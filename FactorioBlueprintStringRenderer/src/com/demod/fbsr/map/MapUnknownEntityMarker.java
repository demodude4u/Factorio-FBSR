package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.MultipleGradientPaint.CycleMethod;

import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapUnknownEntityMarker extends MapRenderable {
	public static final Font FONT = FBSR.getGuiStyle().FONT_BP_BOLD.deriveFont(0.8f);
	public static final Stroke STROKE = new BasicStroke(0.1f);

	private final MapPosition position;
	private final Color color;
	private final Color stripeColor; // secondary color for stripes

	public MapUnknownEntityMarker(MapPosition position, Color color, Color stripeColor) {
		super(Layer.ENTITY_INFO_ICON_ABOVE);
		this.position = position;
		this.color = color;
		this.stripeColor = stripeColor;
	}

	@Override
	public void render(Graphics2D g) {
		double x = position.getX();
		double y = position.getY();
		AffineTransform pat = g.getTransform();
		try {
			g.translate(x, y);
			drawNormalized(g, color, stripeColor);
		} finally {
			g.setTransform(pat);
		}
	}

	public static void drawNormalized(Graphics2D g, Color color, Color secondaryColor) {
		Stroke ps = g.getStroke();
		Paint pp = g.getPaint();

		Ellipse2D.Double shape = new Ellipse2D.Double(-0.5, -0.5, 1, 1);

		float pitch = 0.1f;
		float[] fractions = new float[] { 0f, 0.49f, 0.5f, 1f };
		Color[] colors = new Color[] { color, color, secondaryColor, secondaryColor };
		LinearGradientPaint lgp = new LinearGradientPaint(
				0f, 0f, pitch * 0.5f, pitch,
				fractions, colors,
				CycleMethod.REFLECT);

		g.setPaint(lgp);
		g.fill(shape);

		g.setPaint(pp);
		g.setColor(color.brighter().brighter());
		g.setStroke(STROKE);
		g.draw(shape);
		g.setFont(FONT);
		g.drawString("?", -0.175f, 0.3f);
		g.setStroke(ps);
	}
}
