package com.demod.fbsr.gui.part;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.RichText;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUIAlign;

public class GUIRichText extends GUIPart {
	public final RichText text;
	public final Font font;
	public final Color color;
	public final GUIAlign align;

	public GUIRichText(GUIBox box, String text, Font font, Color color, GUIAlign align, ModdingResolver resolver) {
		super(box);
		this.text = new RichText(text, resolver);
		this.font = font;
		this.color = color;
		this.align = align;
	}

	@Override
	public void render(Graphics2D g) {
		Font prevFont = g.getFont();
		Color prevColor = g.getColor();

		try {
			g.setFont(font);
			g.setColor(color);

			double textWidth = getTextWidth(g);
			int textAscent = g.getFontMetrics().getAscent();
			int textDescent = g.getFontMetrics().getDescent();
			int textHeight = textAscent + textDescent;

			int textX = box.x + (int) ((box.width - textWidth) * align.getHorizontalFactor());
			int textY = box.y + textAscent + (int) ((box.height - textHeight) * align.getVerticalFactor());

			text.draw(g, textX, textY);
		} finally {
			g.setFont(prevFont);
			g.setColor(prevColor);
		}
	}

	public double getTextWidth(Graphics2D g) {
		Font prevFont = g.getFont();
		try {
			g.setFont(font);
			return text.getTextWidth(g);
		} finally {
			g.setFont(prevFont);
		}
	}
}
