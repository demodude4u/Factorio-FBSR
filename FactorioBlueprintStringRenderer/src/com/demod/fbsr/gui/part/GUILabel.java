package com.demod.fbsr.gui.part;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIBox;

public class GUILabel extends GUIPart {

	public final String text;
	public final Font font;
	public final Color color;

	public GUILabel(GUIBox box, String text, Font font, Color color) {
		super(box);
		this.text = text;
		this.font = font;
		this.color = color;
	}

	@Override
	public void render(Graphics2D g) {
		Font prevFont = g.getFont();
		Color prevColor = g.getColor();
		try {
			g.setFont(font);
			g.setColor(color);
			g.drawString(text, box.x, box.y + g.getFontMetrics(font).getHeight());
		} finally {
			g.setFont(prevFont);
			g.setColor(prevColor);
		}
	}
}
