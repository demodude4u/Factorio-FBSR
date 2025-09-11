package com.demod.fbsr.gui.part;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.RichText;
import com.demod.fbsr.gui.GUIAlign;
import com.demod.fbsr.gui.GUIBox;

public class GUIRichTextArea extends GUIPart {
	public String rawText;
	public ModdingResolver resolver;
	public Font font;
	public Color color;
	public GUIAlign align;

	public GUIRichTextArea(GUIBox box, String text, Font font, Color color, GUIAlign align, ModdingResolver resolver) {
		super(box);
		this.rawText = text != null ? text : "";
		this.resolver = resolver;
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

			int ascent = g.getFontMetrics().getAscent();
			int descent = g.getFontMetrics().getDescent();
			int lineHeight = ascent + descent;

			List<Line> lines = wrapLines(g, box.width);
			int limitedLineCount = Math.min(lines.size(), box.height / lineHeight);

			double blockHeight = limitedLineCount * (double) lineHeight;
			int startBaseline = box.y + ascent + (int) ((box.height - blockHeight) * align.getVerticalFactor());
			int bottomLimit = box.y + box.height;

			for (int i = 0; i < limitedLineCount; i++) {
				int baseline = startBaseline + i * lineHeight;

				Line line = lines.get(i);
				int x = box.x + (int) ((box.width - line.width) * align.getHorizontalFactor());
				line.rich.draw(g, x, baseline);
			}
		} finally {
			g.setFont(prevFont);
			g.setColor(prevColor);
		}
	}

	public double getTextWidth(Graphics2D g) {
		Font prevFont = g.getFont();
		try {
			g.setFont(font);
			List<Line> lines = wrapLines(g, box.width);
			double max = 0;
			for (Line l : lines) {
				if (l.width > max)
					max = l.width;
			}
			return max;
		} finally {
			g.setFont(prevFont);
		}
	}

    public int getLineCount(Graphics2D g) {
        Font prevFont = g.getFont();
        try {
            g.setFont(font);
            List<Line> lines = wrapLines(g, box.width);
            return lines.size();
        } finally {
            g.setFont(prevFont);
        }
    }

	private List<Line> wrapLines(Graphics2D g, int maxWidth) {
		List<Line> all = new ArrayList<>();
		String[] paras = rawText.split("\n", -1); // preserve empty lines
		for (String para : paras) {
			all.addAll(wrapParagraph(g, para, maxWidth));
		}
		return all;
	}

	private List<Line> wrapParagraph(Graphics2D g, String para, int maxWidth) {
		List<Line> out = new ArrayList<>();
		if (para.isEmpty()) {
			RichText empty = new RichText("", resolver);
			out.add(new Line(empty, 0));
			return out;
		}

		int n = para.length();
		int pos = 0;
		while (pos < n) {
			int low = 1;
			int high = n - pos;
			int fit = 0;
			while (low <= high) {
				int mid = (low + high) / 2;
				String candidate = para.substring(pos, pos + mid);
				double w = new RichText(candidate, resolver).getTextWidth(g);
				if (w <= maxWidth) {
					fit = mid;
					low = mid + 1;
				} else {
					high = mid - 1;
				}
			}

			if (fit == 0) {
				// Force at least one character to avoid infinite loops; it may overflow visually if a single glyph is wider than the box.
				String s = para.substring(pos, pos + 1);
				RichText r = new RichText(s, resolver);
				double w = r.getTextWidth(g);
				out.add(new Line(r, w));
				pos += 1;
				continue;
			}

			// Prefer breaking at whitespace within the fitted span
			int breakAt = lastBreakableIndex(para, pos, pos + fit);
			if (breakAt >= pos) {
				fit = breakAt - pos + 1; // include the whitespace at the break
			}

			// Trim trailing whitespace on the visual line
			int end = pos + fit;
			int trimmedEnd = end;
			while (trimmedEnd > pos && Character.isWhitespace(para.charAt(trimmedEnd - 1))) {
				trimmedEnd--;
			}
			String lineStr = para.substring(pos, trimmedEnd);
			RichText lineRich = new RichText(lineStr, resolver);
			double lineWidth = lineRich.getTextWidth(g);
			out.add(new Line(lineRich, lineWidth));

			// Advance past any additional whitespace
			pos = end;
			while (pos < n && Character.isWhitespace(para.charAt(pos))) {
				pos++;
			}
		}
		return out;
	}

	private static int lastBreakableIndex(String s, int start, int endExclusive) {
        if (s.length() == endExclusive) {
            return -1;
        }
		for (int i = endExclusive - 1; i >= start; i--) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c)) {
				return i;
			}
		}
		return -1;
	}

	private static final class Line {
		final RichText rich;
		final double width;

		Line(RichText rich, double width) {
			this.rich = rich;
			this.width = width;
		}
	}
}
