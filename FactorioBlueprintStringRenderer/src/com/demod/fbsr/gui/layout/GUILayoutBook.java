package com.demod.fbsr.gui.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import com.demod.dcba.CommandReporting;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprintBook;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.part.GUIImage;
import com.demod.fbsr.gui.part.GUILabel;
import com.demod.fbsr.gui.part.GUILabel.Align;
import com.demod.fbsr.gui.part.GUIPanel;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;

public class GUILayoutBook {

	public static class ImageBlock {
		public final int rows;
		public final int cols;
		public final BufferedImage image;

		private final Rectangle location;

		public ImageBlock(int rows, int cols, BufferedImage image) {
			this.rows = rows;
			this.cols = cols;
			this.image = image;

			location = new Rectangle(cols, rows);
		}

	}

	public static final GUISize BP_CELL_SIZE = new GUISize(400, 300);
	public static final GUISize BP_IMAGE_MIN = new GUISize(BP_CELL_SIZE.width * 2, BP_CELL_SIZE.height * 2);
	public static final GUISize BP_IMAGE_MAX = new GUISize(BP_CELL_SIZE.width * 4, BP_CELL_SIZE.height * 4);

	public static final double DISCORD_IMAGE_RATIO = GUILayoutBlueprint.DISCORD_IMAGE_SIZE.width
			/ GUILayoutBlueprint.DISCORD_IMAGE_SIZE.height;

	private static Rectangle computeBoundingBox(List<ImageBlock> blocks) {
		if (blocks.isEmpty())
			return new Rectangle(0, 0, 0, 0);

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (ImageBlock block : blocks) {
			Rectangle r = block.location;
			if (r.x < minX)
				minX = r.x;
			if (r.y < minY)
				minY = r.y;
			int rMaxX = r.x + r.width;
			int rMaxY = r.y + r.height;
			if (rMaxX > maxX)
				maxX = rMaxX;
			if (rMaxY > maxY)
				maxY = rMaxY;
		}

		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	private static boolean overlapsAny(List<ImageBlock> blocks, int i, int x, int y) {
		Rectangle current = blocks.get(i).location;
		Rectangle testRect = new Rectangle(x, y, current.width, current.height);

		for (int j = 0; j < i; j++) {
			Rectangle placed = blocks.get(j).location;
			if (testRect.intersects(placed)) {
				return true;
			}
		}
		return false;
	}

	public static Rectangle packBlocks(List<ImageBlock> blocks, double targetRatio) {
		// Compute total area and largest rectangle dimensions
		int totalArea = 0;
		int width = 0;
		int height = 0;
		for (ImageBlock block : blocks) {
			Rectangle r = block.location;
			totalArea += r.width * r.height;
			if (r.width > width) {
				width = r.width;
				height = (int) Math.ceil(width / targetRatio);
			}
			if (r.height > height) {
				width = (int) Math.ceil(height * targetRatio);
				height = r.height;
			}
		}

		int areaBasedWidth = (int) Math.ceil(Math.sqrt(totalArea * targetRatio));
		int areaBasedHeight = (int) Math.ceil(areaBasedWidth / targetRatio);
		width = Math.max(width, areaBasedWidth);
		height = Math.max(height, areaBasedHeight);

		int maxAttempts = 1000;
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			if (tryPlaceRectangles(blocks, width, height)) {
				// Success
				return computeBoundingBox(blocks);
			} else {
				// Adjust dimensions slightly while trying to preserve ratio
				double currentRatio = (double) width / (double) height;
				if (currentRatio < targetRatio) {
					width++;
				} else {
					height++;
				}
			}
		}

		throw new RuntimeException("Could not pack rectangles within max attempts.");
	}

	/**
	 * Attempts to place all rectangles in the given width and height. O(n²)
	 * approach: - For each rectangle, try every (x, y) position until a valid
	 * non-overlapping spot is found. - If no spot is found, return false.
	 */
	private static boolean tryPlaceRectangles(List<ImageBlock> blocks, int width, int height) {
		for (int i = 0; i < blocks.size(); i++) {
			Rectangle r = blocks.get(i).location;
			boolean placed = false;
			for (int y = 0; y <= height - r.height && !placed; y++) {
				for (int x = 0; x <= width - r.width && !placed; x++) {
					if (!overlapsAny(blocks, i, x, y)) {
						r.setLocation(x, y);
						placed = true;
					}
				}
			}

			if (!placed) {
				return false; // could not place this rectangle
			}
		}
		return true;
	}

	private BSBlueprintBook book;
	private CommandReporting reporting;
	private List<RenderResult> results;
	private List<ImageBlock> blocks;
	private Rectangle packBounds;

	private void drawFrame(Graphics2D g, GUIBox bounds) {
		int titleHeight = 50;
		int creditHeight = 25;

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER);
		panel.render(g);

		drawTitleBar(g, bounds.cutTop(titleHeight));
		drawImagePane(g, bounds.shrinkTop(titleHeight));

		GUIBox creditBounds = bounds.cutLeft(175).cutBottom(creditHeight).expandTop(8).shrink(0, 8, 8, 0);
		GUIPanel creditPanel = new GUIPanel(creditBounds.expandBottom(2), GUIStyle.FRAME_LIGHT_INNER);
		creditPanel.render(g);
		GUILabel lblCredit = new GUILabel(creditBounds, "BlueprintBot " + FBSR.getVersion(),
				GUIStyle.FONT_BP_BOLD.deriveFont(16f), Color.GRAY, Align.CENTER);
		lblCredit.render(g);
	}

	private void drawImagePane(Graphics2D g, GUIBox bounds) {
		bounds = bounds.shrink(0, 24, 24, 24);

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		panel.render(g);

		AffineTransform xform = g.getTransform();
		double renderScaleX = xform.getScaleX();
		double renderScaleY = xform.getScaleY();

		double cellOffsetX = -20 / renderScaleX;
		double cellOffsetY = -20 / renderScaleY;
		double cellWidth = (BP_CELL_SIZE.width + 20) / renderScaleX;
		double cellHeight = (BP_CELL_SIZE.height + 20) / renderScaleY;

		for (ImageBlock block : blocks) {
			int centerX = (int) (bounds.x + bounds.width / 2
					+ (-packBounds.width / 2.0 - packBounds.x + block.location.x + block.location.width / 2.0)
							* cellWidth
					+ cellOffsetX);
			int centerY = (int) (bounds.y + bounds.height / 2
					+ (-packBounds.height / 2.0 - packBounds.y + block.location.y + block.location.height / 2.0)
							* cellHeight
					+ cellOffsetY);
			GUIImage image = new GUIImage(new GUIBox(centerX, centerY, 0, 0), block.image, true);
			image.render(g);

//			g.setColor(new Color(block.hashCode()));
//			g.drawRect(
//					10 + (int) (bounds.x + bounds.width / 2
//							+ (-packBounds.width / 2.0 - packBounds.x + block.location.x) * cellWidth + cellOffsetX),
//					10 + (int) (bounds.y + bounds.height / 2
//							+ (-packBounds.height / 2.0 - packBounds.y + block.location.y) * cellHeight + cellOffsetY),
//					-20 + (int) (block.location.width * cellWidth), -20 + (int) (block.location.height * cellHeight));
		}

		Table<Integer, Integer, Integer> groupings = ArrayTable.create(
				IntStream.rangeClosed(packBounds.y, packBounds.y + packBounds.height).mapToObj(i -> (Integer) i)
						.toList(),
				IntStream.rangeClosed(packBounds.x, packBounds.x + packBounds.width).mapToObj(i -> (Integer) i)
						.toList());
		for (int i = 0; i < blocks.size(); i++) {
			ImageBlock block = blocks.get(i);
			for (int row = block.location.y; row < block.location.y + block.location.height; row++) {
				for (int col = block.location.x; col < block.location.x + block.location.width; col++) {
					groupings.put(row, col, i);
				}
			}
		}

//		for (int row = packBounds.y; row < packBounds.y + packBounds.height; row++) {
//			for (int col = packBounds.x; col < packBounds.x + packBounds.width; col++) {
//				System.out.print(Optional.ofNullable(groupings.get(row, col)).map(i -> "" + i).orElse("."));
//			}
//			System.out.println();
//		}

		int pipeX = (int) (bounds.x + bounds.width / 2 + (-packBounds.width / 2.0) * cellWidth) - 4;
		int pipeY = (int) (bounds.y + bounds.height / 2 + (-packBounds.height / 2.0) * cellHeight) - 4;
		GUIStyle.PIPE.renderDynamicGrid(g, pipeX, pipeY, cellWidth, cellHeight, packBounds, groupings);

	}

	private void drawTitleBar(Graphics2D g, GUIBox bounds) {
		GUILabel lblTitle = new GUILabel(bounds.shrinkBottom(8).shrinkLeft(24),
				book.label.orElse("Untitled Blueprint Book"), GUIStyle.FONT_BP_BOLD.deriveFont(24f),
				GUIStyle.FONT_BP_COLOR, Align.CENTER_LEFT);
		lblTitle.render(g);

		int startX = bounds.x + (int) (lblTitle.getTextWidth(g) + 44);
		int endX = bounds.x + bounds.width - 24;
		GUIPipeFeature pipe = GUIStyle.DRAG_LINES;
		for (int x = endX - pipe.size; x >= startX; x -= pipe.size) {
			pipe.renderVertical(g, x, bounds.y + 10, bounds.y + bounds.height - 10);
		}
	}

	public BufferedImage generateDiscordImage() {

		double renderScale = 0.5;
		double uiScale = 1;

		// Render Images
		blocks = new ArrayList<>();
		results = new ArrayList<>();
		for (BSBlueprint blueprint : book.getAllBlueprints()) {

			int minWidth = (int) (BP_IMAGE_MIN.width * renderScale);
			int minHeight = (int) (BP_IMAGE_MIN.height * renderScale);
			int maxWidth = (int) (BP_IMAGE_MAX.width * renderScale);
			int maxHeight = (int) (BP_IMAGE_MAX.height * renderScale);

			RenderRequest request = new RenderRequest(blueprint, reporting);
			request.setMinWidth(OptionalInt.of(minWidth));
			request.setMinHeight(OptionalInt.of(minHeight));
			request.setMaxWidth(OptionalInt.of(maxWidth));
			request.setMaxHeight(OptionalInt.of(maxHeight));
			request.setBackground(Optional.empty());
			request.setGridLines(Optional.empty());
			request.setMaxScale(OptionalDouble.of(0.5));

			RenderResult result = FBSR.renderBlueprint(request);
			results.add(result);

			int rows = (result.image.getHeight() + BP_CELL_SIZE.height - 1) / BP_CELL_SIZE.height;
			int cols = (result.image.getWidth() + BP_CELL_SIZE.width - 1) / BP_CELL_SIZE.width;
			blocks.add(new ImageBlock(rows, cols, result.image));
		}

		packBounds = packBlocks(blocks, DISCORD_IMAGE_RATIO);

		int imageWidth = BP_CELL_SIZE.width * packBounds.width;
		int imageHeight = BP_CELL_SIZE.height * packBounds.height;

		// Pipe gaps between cells
		imageWidth += (packBounds.width + 1) * 20 * uiScale;
		imageHeight += (packBounds.height + 1) * 20 * uiScale;
		// Framing
		imageWidth += 48 * uiScale;
		imageHeight += 78 * uiScale;

		BufferedImage ret = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		GUIBox bounds = new GUIBox(0, 0, (int) (ret.getWidth() / uiScale), (int) (ret.getHeight() / uiScale));

		Graphics2D g = ret.createGraphics();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.scale(uiScale, uiScale);

			drawFrame(g, bounds);
		} finally {
			g.dispose();
		}

		return ret;

	}

	public List<RenderResult> getResults() {
		return results;
	}

	public void setBook(BSBlueprintBook book) {
		this.book = book;
	}

	public void setReporting(CommandReporting reporting) {
		this.reporting = reporting;
	}

}
