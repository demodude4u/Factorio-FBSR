package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import com.demod.fbsr.Layer;
import com.demod.fbsr.RichText;

public class MapText extends MapRenderable {

	private final MapPosition position;
	private final int angle;
	protected final Font font;
	protected final Color color;
	protected RichText text;

	public MapText(Layer layer, MapPosition position, int angle, Font font, Color color, String string) {
		super(layer);
		this.position = position;
		this.angle = angle;
		this.font = font;
		this.color = color;
		text = new RichText(string);
	}

	@Override
	public void render(Graphics2D g) {
		if (angle != 0) {
			AffineTransform pat = g.getTransform();

			g.setFont(font);
			float textX = position.xfp;
			float textY = position.yfp;

			g.translate(textX, textY);
			g.rotate(angle);

			g.setColor(Color.darkGray);
			text.drawShadow(g, 0.05f, 0.05f);
			g.setColor(color);
			text.draw(g, 0f, 0f);
			
			g.setTransform(pat);

		} else {
			g.setFont(font);
			float textX = (float) position.getX();
			float textY = (float) position.getY();
			g.setColor(Color.darkGray);
			text.drawShadow(g, textX + 0.05f, textY + 0.05f);
			g.setColor(color);
			text.draw(g, textX, textY);
		}
	}

	public void setString(String string) {
		this.text = new RichText(string);
	}
}
