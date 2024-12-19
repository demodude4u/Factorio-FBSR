package com.demod.fbsr.gui.feature;

import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISpacing;

public class GUISliceFeature extends GUISourcedFeature {
	public static GUISliceFeature inner(String filename, GUIBox source, GUISpacing slice) {
		return new GUISliceFeature(GUISpacing.NONE, slice, filename, source, slice);
	}

	public static GUISliceFeature outer(String filename, GUIBox source, GUISpacing slice) {
		return new GUISliceFeature(slice, GUISpacing.NONE, filename, source, slice);
	}

	public final GUISpacing slice;
	public final int[] dx;
	public final int[] dy;
	public final int[] sx;
	public final int[] sy;

	public GUISliceFeature(GUISpacing margin, GUISpacing padding, String filename, GUIBox source, GUISpacing slice) {
		super(filename, source);
		this.slice = slice;

		dx = new int[] { -margin.left, padding.left, -padding.right, margin.right };
		dy = new int[] { -margin.top, padding.top, -padding.bottom, margin.bottom };
		sx = new int[] { source.x, source.x + slice.left, source.x + source.width - slice.right,
				source.x + source.width };
		sy = new int[] { source.y, source.y + slice.top, source.y + source.height - slice.bottom,
				source.y + source.height };
	}

	public void render(Graphics2D g, GUIBox r) {
		int rx1 = r.x;
		int rx2 = rx1 + r.width;
		int ry1 = r.y;
		int ry2 = ry1 + r.height;
		int[] rx = { rx1, rx1, rx2, rx2 };
		int[] ry = { ry1, ry1, ry2, ry2 };

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int dx1 = rx[col] + dx[col];
				int dx2 = rx[col + 1] + dx[col + 1];
				int dy1 = ry[row] + dy[row];
				int dy2 = ry[row + 1] + dy[row + 1];
				int sx1 = sx[col];
				int sx2 = sx[col + 1];
				int sy1 = sy[row];
				int sy2 = sy[row + 1];
				g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
			}
		}
	}

}