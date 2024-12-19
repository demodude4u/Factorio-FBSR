package com.demod.fbsr.gui.feature;

import java.awt.Graphics2D;
import java.util.stream.IntStream;

import com.demod.fbsr.gui.GUIBox;

public class GUIPipeFeature extends GUISourcedFeature {
	public static final int N = 0b0001;
	public static final int E = 0b0010;
	public static final int NE = 0b0011;
	public static final int S = 0b0100;
	public static final int NS = 0b0101;
	public static final int SE = 0b0110;
	public static final int NSE = 0b0111;
	public static final int W = 0b1000;
	public static final int NW = 0b1001;
	public static final int EW = 0b1010;
	public static final int NEW = 0b1011;
	public static final int SW = 0b1100;
	public static final int NSW = 0b1101;
	public static final int SEW = 0b1110;
	public static final int NSEW = 0b1111;

	public static GUIPipeFeature dragLines(String filename, GUIBox source) {
		return new GUIPipeFeature(filename, source, new int[] { //
				-1, // ....
				0, // ...N
				-1, // ..E.
				-1, // ..EN
				2, // .S..
				1, // .S.N
				-1, // .SE.
				-1, // .SEN
				-1, // W...
				-1, // W..N
				-1, // W.E.
				-1, // W.EN
				-1, // WS..
				-1, // WS.N
				-1, // WSE.
				-1,// WSEN
		});
	}

	public static GUIPipeFeature full(String filename, GUIBox source) {
		return new GUIPipeFeature(filename, source, new int[] { //
				-1, // ....
				13, // ...N
				14, // ..E.
				4, // ..EN
				11, // .S..
				0, // .S.N
				5, // .SE.
				7, // .SEN
				12, // W...
				3, // W..N
				1, // W.E.
				6, // W.EN
				2, // WS..
				9, // WS.N
				8, // WSE.
				10,// WSEN
		});
	}

	public final int size;
	public final int[] indices;
	public final int[] sx;

	public GUIPipeFeature(String filename, GUIBox source, int[] indices) {
		super(filename, source);
		size = source.height;
		this.indices = indices;
		sx = IntStream.of(indices).map(i -> source.x + size * i).toArray();
	}

	public void renderBox(Graphics2D g, GUIBox r) {
		int x1 = r.x;
		int x2 = r.x + size;
		int x3 = r.width - size;
		int x4 = r.width;
		int y1 = r.y;
		int y2 = r.y + size;
		int y3 = r.height - size;
		int y4 = r.height;

		int sy1 = source.y;
		int sy2 = source.y + size;

		// Top Left Corner
		g.drawImage(image, x1, y1, x2, y2, sx[SE], sy1, sx[SE] + size, sy2, null);
		// Top Side
		g.drawImage(image, x2, y1, x3, y2, sx[EW], sy1, sx[EW] + 1, sy2, null);
		// Top Right Corner
		g.drawImage(image, x3, y1, x4, y2, sx[SW], sy1, sx[SW] + size, sy2, null);
		// Right Side
		g.drawImage(image, x3, y2, x4, y3, sx[NS], sy1, sx[NS] + size, sy1 + 1, null);
		// Bottom Right Corner
		g.drawImage(image, x3, y3, x4, y4, sx[NW], sy1, sx[NW] + size, sy2, null);
		// Bottom Side
		g.drawImage(image, x2, y3, x3, y4, sx[EW], sy1, sx[EW] + 1, sy2, null);
		// Bottom Left Corner
		g.drawImage(image, x1, y3, x2, y4, sx[NE], sy1, sx[NE] + size, sy2, null);
		// Left Side
		g.drawImage(image, x1, y2, x2, y3, sx[NS], sy1, sx[NS] + size, sy1 + 1, null);
	}

	public void renderVertical(Graphics2D g, int x, int yStart, int yEnd) {
		int x1 = x;
		int x2 = x + size;
		int y1 = yStart;
		int y2 = yStart + size;
		int y3 = yEnd - size;
		int y4 = yEnd;

		int sy1 = source.y;
		int sy2 = source.y + size;

		// Top
		g.drawImage(image, x1, y1, x2, y2, sx[S], sy1, sx[S] + size, sy2, null);
		// Middle
		g.drawImage(image, x1, y2, x2, y3, sx[NS], sy1, sx[NS] + size, sy1 + 1, null);
		// Bottom
		g.drawImage(image, x1, y3, x2, y4, sx[N], sy1, sx[N] + size, sy2, null);
	}
}