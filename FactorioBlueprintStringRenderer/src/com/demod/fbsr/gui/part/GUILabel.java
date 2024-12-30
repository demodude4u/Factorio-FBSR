package com.demod.fbsr.gui.part;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIBox;

public class GUILabel extends GUIPart {

	public enum Align {
		TOP_LEFT(0, 0), TOP_CENTER(0.5, 0), TOP_RIGHT(1, 0), CENTER_LEFT(0, 0.5), CENTER(0.5, 0.5),
		CENTER_RIGHT(1, 0.5), BOTTOM_LEFT(0, 1), BOTTOM_CENTER(0.5, 1), BOTTOM_RIGHT(1, 1);

		private final double horizontalFactor;
		private final double verticalFactor;

		Align(double horizontalFactor, double verticalFactor) {
			this.horizontalFactor = horizontalFactor;
			this.verticalFactor = verticalFactor;
		}

		public double getHorizontalFactor() {
			return horizontalFactor;
		}

		public double getVerticalFactor() {
			return verticalFactor;
		}
	}

	public final String text;
	public final Font font;
	public final Color color;
	public final Align align;

	public GUILabel(GUIBox box, String text, Font font, Color color, Align align) {
		super(box);
		this.text = text;
		this.font = font;
		this.color = color;
		this.align = align;
	}

	public double getTextWidth(Graphics2D g) {
		return g.getFontMetrics(font).stringWidth(text) / g.getTransform().getScaleX();
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
