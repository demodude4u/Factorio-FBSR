package com.demod.fbsr.gui.feature;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Optional;
import java.util.function.ToIntBiFunction;
import java.util.stream.IntStream;

import com.demod.fbsr.gui.GUIBox;
import com.google.common.collect.Table;

public class GUIPipeFeature extends GUISourcedFeature {
	private static final int[] INDICES_DRAGLINES = new int[] { //
			-1, // ....
			2, // ...N
			-1, // ..E.
			-1, // ..EN
			0, // .S..
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
	};

	private static final int[] INDICES_FULL = new int[] { //
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
	};

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

	// index = bitset of group change on the (W, S, E, W) side boundaries
	public static final int[] DYNAMIC_GRID_MAP = { //
			-1, // 0
			-1, // 1
			-1, // 2
			NE, // 3
			-1, // 4
			NS, // 5
			SE, // 6
			NSE, // 7
			-1, // 8
			NW, // 9
			EW, // 10
			NEW, // 11
			SW, // 12
			NSW, // 13
			SEW, // 14
			NSEW,// 15
	};

	public static GUIPipeFeature dragLines(String filename, GUIBox source) {
		return new GUIPipeFeature(filename, source, INDICES_DRAGLINES);
	}

	public static GUIPipeFeature full(String filename, GUIBox source) {
		return new GUIPipeFeature(filename, source, INDICES_FULL);
	}

	public final int size;
	public final int[] indices;
	public final int[] sx;

	public GUIPipeFeature(String filename, GUIBox source, int[] indices) {
		super(filename, source);
		size = source.height;
		this.indices = indices;
		sx = IntStream.of(indices).map(i -> size * i).toArray();
	}

	public void renderBox(Graphics2D g, GUIBox r) {
		int x1 = r.x;
		int x2 = r.x + size;
		int x3 = r.x + r.width - size;
		int x4 = r.x + r.width;
		int y1 = r.y;
		int y2 = r.y + size;
		int y3 = r.y + r.height - size;
		int y4 = r.y + r.height;

		int sy1 = 0;
		int sy2 = size;

		// Top Left Corner
		drawImage(g, x1, y1, x2, y2, sx[SE], sy1, sx[SE] + size, sy2);
		// Top Side
		drawImage(g, x2, y1, x3, y2, sx[EW], sy1, sx[EW] + 1, sy2);
		// Top Right Corner
		drawImage(g, x3, y1, x4, y2, sx[SW], sy1, sx[SW] + size, sy2);
		// Right Side
		drawImage(g, x3, y2, x4, y3, sx[NS], sy1, sx[NS] + size, sy1 + 1);
		// Bottom Right Corner
		drawImage(g, x3, y3, x4, y4, sx[NW], sy1, sx[NW] + size, sy2);
		// Bottom Side
		drawImage(g, x2, y3, x3, y4, sx[EW], sy1, sx[EW] + 1, sy2);
		// Bottom Left Corner
		drawImage(g, x1, y3, x2, y4, sx[NE], sy1, sx[NE] + size, sy2);
		// Left Side
		drawImage(g, x1, y2, x2, y3, sx[NS], sy1, sx[NS] + size, sy1 + 1);
	}

	// Draws pipes based on matching cell grouping numbers in the grouping grid.
	public void renderDynamicGrid(Graphics2D g, int x, int y, double cellWidth, double cellHeight, Rectangle bounds,
			Table<Integer, Integer, Integer> groupings) {

		int sy1 = 0;
		int sy2 = size;

		final int boundsX1 = bounds.x;
		final int boundsY1 = bounds.y;
		final int boundsX2 = boundsX1 + bounds.width;
		final int boundsY2 = boundsY1 + bounds.height;

		ToIntBiFunction<Integer, Integer> lookup = (row, col) -> {
			if (col < boundsX1 || row < boundsY1 || col >= boundsX2 || row >= boundsY2) {
				return -1;
			}
			return Optional.ofNullable(groupings.get(row, col)).orElse(-1);
		};

		for (int row = boundsY1; row <= boundsY2; row++) {
			for (int col = boundsX1; col <= boundsX2; col++) {

				// Quadrants
				int groupNW = lookup.applyAsInt(row - 1, col - 1);
				int groupNE = lookup.applyAsInt(row - 1, col);
				int groupSE = lookup.applyAsInt(row, col);
				int groupSW = lookup.applyAsInt(row, col - 1);

				// Comparing quadrants
				boolean diffN = groupNW != groupNE;
				boolean diffE = groupSE != groupNE;
				boolean diffS = groupSW != groupSE;
				boolean diffW = groupNW != groupSW;

				int id = 0;
				id |= diffN ? 0b0001 : 0;
				id |= diffE ? 0b0010 : 0;
				id |= diffS ? 0b0100 : 0;
				id |= diffW ? 0b1000 : 0;
				int mode = DYNAMIC_GRID_MAP[id];

				int lineX = (int) (x + (col - boundsX1) * cellWidth);
				int lineY = (int) (y + (row - boundsY1) * cellHeight);

				if (mode != -1 && mode != NS && mode != EW) {
					int x1 = lineX;
					int y1 = lineY;
					int x2 = lineX + size;
					int y2 = lineY + size;
					drawImage(g, x1, y1, x2, y2, sx[mode], sy1, sx[mode] + size, sy2);

					lineX += size;
					lineY += size;
				}

				if (diffS) { // vertical line
					int x1 = (int) (x + (col - boundsX1) * cellWidth);
					int y1 = lineY;
					int x2 = x1 + size;
					int y2 = (int) (y + (row + 1 - boundsY1) * cellHeight);
					drawImage(g, x1, y1, x2, y2, sx[NS], sy1, sx[NS] + size, sy1 + 1);
				}

				if (diffE) { // horizontal line
					int x1 = lineX;
					int y1 = (int) (y + (row - boundsY1) * cellHeight);
					int x2 = (int) (x + (col + 1 - boundsX1) * cellWidth);
					int y2 = y1 + size;
					drawImage(g, x1, y1, x2, y2, sx[EW], sy1, sx[EW] + 1, sy2);
				}
			}
		}
	}

	public void renderVertical(Graphics2D g, int x, int yStart, int yEnd) {
		int x1 = x;
		int x2 = x + size;
		int y1 = yStart;
		int y2 = yStart + size;
		int y3 = yEnd - size;
		int y4 = yEnd;

		int sy1 = 0;
		int sy2 = size;

		// Top
		drawImage(g, x1, y1, x2, y2, sx[S], sy1, sx[S] + size, sy2);
		// Middle
		drawImage(g, x1, y2, x2, y3, sx[NS], sy1, sx[NS] + size, sy1 + 1);
		// Bottom
		drawImage(g, x1, y3, x2, y4, sx[N], sy1, sx[N] + size, sy2);
	}
}