package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.demod.fbsr.Layer;

public class MapText extends MapRenderable {

	protected final Font font;
	protected final Color color;
	protected final String string;

	public MapText(Layer layer, MapPosition position, Font font, Color color, String string) {
		super(layer, position);
		this.font = font;
		this.color = color;
		this.string = string;
	}

	@Override
	public void render(Graphics2D g) {
		g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
		float textX = (float) position.getX();
		float textY = (float) position.getY();
		g.setColor(Color.darkGray);
		g.drawString(string, textX + 0.05f, textY + 0.05f);
		g.setColor(color);
		g.drawString(string, textX, textY);
	}

}
