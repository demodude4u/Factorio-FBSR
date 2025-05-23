package com.demod.fbsr.gui.part;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIAlign;
import com.demod.fbsr.gui.GUIBox;

public class GUILabel extends GUIPart {

	public final String text;
	public final Font font;
	public final Color color;
	public final GUIAlign align;

	public GUILabel(GUIBox box, String text, Font font, Color color, GUIAlign align) {
		super(box);
		this.text = text;
		this.font = font;
		this.color = color;
		this.align = align;
	}

	public double getTextWidth(Graphics2D g) {
		return g.getFontMetrics(font).stringWidth(text);
	}

	@Override
	public void render(Graphics2D g) {
		Font prevFont = g.getFont();
		Color prevColor = g.getColor();

		try {
			g.setFont(font);
			g.setColor(color);

			int textWidth = g.getFontMetrics().stringWidth(text);
			int textAscent = g.getFontMetrics().getAscent();
			int textDescent = g.getFontMetrics().getDescent();
			int textHeight = textAscent + textDescent;

			int textX = box.x + (int) ((box.width - textWidth) * align.getHorizontalFactor());
			int textY = box.y + textAscent + (int) ((box.height - textHeight) * align.getVerticalFactor());

			g.drawString(text, textX, textY);
		} finally {
			g.setFont(prevFont);
			g.setColor(prevColor);
		}
	}
}
