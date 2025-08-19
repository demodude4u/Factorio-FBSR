package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import com.demod.fbsr.Layer;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.RichText;

public class MapText extends MapRenderable {

	public static final int MIN_SCREEN_SIZE = 16;

	private final MapPosition position;
	private final int angle;
	protected final Font font;
	protected final Color color;
	protected final ModdingResolver resolver;

	private boolean centered;


	protected RichText text;


	public MapText(Layer layer, MapPosition position, int angle, Font font, Color color, String string, boolean centered, ModdingResolver resolver) {
		super(layer);
		this.position = position;
		this.angle = angle;
		this.font = font;
		this.color = color;
		this.resolver = resolver;

		this.centered = centered;

		text = new RichText(string, resolver);
	}

	@Override
	public void render(Graphics2D g) {
		AffineTransform xform = g.getTransform();
		double scaleX = xform.getScaleX();
		double scaleY = xform.getScaleY();
		double effectiveScale = Math.sqrt(scaleX * scaleX + scaleY * scaleY);
		double minSize = MIN_SCREEN_SIZE / effectiveScale;

		// Adjust font size to ensure it is at least minSize
		float adjustedFontSize = (float) Math.max(font.getSize2D(), minSize);

		Font font = this.font;
		if (font.getSize2D() != adjustedFontSize) {
			font = font.deriveFont(adjustedFontSize);
		}
		g.setFont(font);

		float textX = (float) position.getX();
		float textY = (float) position.getY();

		if (angle != 0) {
			AffineTransform pat = xform;

			g.translate(textX, textY);
			g.rotate(angle / 180f * Math.PI);

			if (centered) {
				g.translate(-text.getTextWidth(g)/2.0, 0);
			}

			g.setColor(Color.darkGray);
			text.drawShadow(g, 0.05f, 0.05f);
			g.setColor(color);
			text.draw(g, 0f, 0f);

			g.setTransform(pat);

		} else {

			if (centered) {
				textX -= text.getTextWidth(g) / 2.0;
			}

			g.setColor(Color.darkGray);
			text.drawShadow(g, textX + 0.05f, textY + 0.05f);
			g.setColor(color);
			text.draw(g, textX, textY);
		}
	}

	public void setString(String string) {
		this.text = new RichText(string, resolver);
	}
}
