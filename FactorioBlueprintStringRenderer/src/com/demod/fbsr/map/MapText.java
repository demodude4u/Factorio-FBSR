package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import com.demod.fbsr.Layer;

public class MapText extends MapRenderable {

	private final int angle;
	protected final Font font;
	protected final Color color;
	protected final String string;

	public MapText(Layer layer, MapPosition position, int angle, Font font, Color color, String string) {
		super(layer, position);
		this.angle = angle;
		this.font = font;
		this.color = color;
		this.string = string;
	}

	@Override
	public void render(Graphics2D g) {
		if (angle != 0) {
			AffineTransform pat = g.getTransform();

			g.setFont(font);
			float textX = position.x;
			float textY = position.y;

			g.translate(textX, textY);
			g.rotate(angle);

			g.setColor(Color.darkGray);
			g.drawString(string, 0.05f, 0.05f);
			g.setColor(color);
			g.drawString(string, 0f, 0f);

			g.setTransform(pat);

		} else {
			g.setFont(font);
			float textX = (float) position.getX();
			float textY = (float) position.getY();
			g.setColor(Color.darkGray);
			g.drawString(string, textX + 0.05f, textY + 0.05f);
			g.setColor(color);
			g.drawString(string, textX, textY);
		}
	}

}
