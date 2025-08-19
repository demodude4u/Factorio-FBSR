package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;
import com.demod.fbsr.gui.GUIStyle;

public class MapGrid extends MapRenderable {
	private static final BasicStroke STROKE = new BasicStroke((float) (3 / FBSR.TILE_SIZE));
	public static final Font FONT = FBSR.getGuiStyle().FONT_BP_REGULAR.deriveFont(0.6f);

	private final MapRect3D bounds;
	private final Color color;
	private final boolean showNumbers;

	public MapGrid(MapRect3D bounds, Color color, boolean aboveBelts, boolean showNumbers) {
		super(aboveBelts ? Layer.GRID_ABOVE_BELTS : Layer.GRID);
		this.bounds = bounds;
		this.color = color;
		this.showNumbers = showNumbers;
	}

	@Override
	public void render(Graphics2D g) {
		g.setStroke(STROKE);
		g.setColor(color);
		int x1 = (int) Math.round(bounds.getX1());
		int y1 = (int) Math.round(bounds.getY1());
		int x2 = (int) Math.round(bounds.getX2());
		int y2 = (int) Math.round(bounds.getY2());
		for (double x = x1 + 1; x <= x2 - 1; x++) {
			g.draw(new Line2D.Double(x, y1, x, y2));
		}
		for (double y = y1 + 1; y <= y2 - 1; y++) {
			g.draw(new Line2D.Double(x1, y, x2, y));
		}
		double gridRound = showNumbers ? 0.6 : 0.2;
		g.draw(new RoundRectangle2D.Double(x1, y1, x2 - x1, y2 - y1, gridRound, gridRound));

		if (showNumbers) {
			g.setFont(FONT);
			float tx = 0.18f;
			float ty = 0.68f;
			for (double x = x1 + 1, i = 1; x <= x2 - 2; x++, i++) {
				String strNum = String.format("%02d", (int) Math.round(i) % 100);
				float sx = (float) x + tx;
				float sy1 = y1 + ty;
				float sy2 = y2 - 1 + ty;
				g.drawString(strNum, sx, sy1);
				g.drawString(strNum, sx, sy2);
			}
			for (double y = y1 + 1, i = 1; y <= y2 - 2; y++, i++) {
				String strNum = String.format("%02d", (int) Math.round(i) % 100);
				float sx1 = x1 + tx;
				float sx2 = x2 - 1 + tx;
				float sy = (float) y + ty;
				g.drawString(strNum, sx1, sy);
				g.drawString(strNum, sx2, sy);
			}
		}
	}

}
