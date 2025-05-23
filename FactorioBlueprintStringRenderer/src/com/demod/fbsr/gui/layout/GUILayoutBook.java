package com.demod.fbsr.gui.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.demod.dcba.CommandReporting;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.RichText;
import com.demod.fbsr.RichText.TagToken;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprintBook;
import com.demod.fbsr.bs.BSIcon;
import com.demod.fbsr.composite.TintComposite;
import com.demod.fbsr.gui.GUIAlign;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.part.GUIImage;
import com.demod.fbsr.gui.part.GUILabel;
import com.demod.fbsr.gui.part.GUIPanel;
import com.demod.fbsr.gui.part.GUIPart;
import com.demod.fbsr.gui.part.GUIRichText;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;

public class GUILayoutBook {

	public static class ImageBlock {
		public final int rows;
		public final int cols;
		public final Optional<String> label;
		public final List<BSIcon> icons;
		public final BufferedImage image;

		private final Rectangle location;

		public ImageBlock(int rows, int cols, Optional<String> label, List<BSIcon> icons, BufferedImage image) {
			this.rows = rows;
			this.cols = cols;
			this.label = label;
			this.icons = icons;
			this.image = image;

			location = new Rectangle(cols, rows);
		}

	}

	public static final GUISize BP_CELL_SIZE = new GUISize(200, 150);
	public static final GUISize BP_IMAGE_MIN = new GUISize(BP_CELL_SIZE.width, BP_CELL_SIZE.height);
	public static final GUISize BP_IMAGE_MAX = new GUISize(BP_CELL_SIZE.width * 8, BP_CELL_SIZE.height * 8);

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
	private Composite pc;
	private Composite tint;

	private boolean spaceAge;

	private List<String> mods;

	private void drawFrame(Graphics2D g, GUIBox bounds) {
		g.setComposite(tint);

		int titleHeight = 50;

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER);
		renderTinted(g, panel);

		drawTitleBar(g, bounds.cutTop(titleHeight));
		drawImagePane(g, bounds.shrinkTop(titleHeight));

		GUIBox creditBounds = bounds.cutRight(190).cutBottom(24).expandTop(8).cutTop(16).cutLeft(160);
		GUIPanel creditPanel = new GUIPanel(creditBounds, GUIStyle.FRAME_TAB);
		renderTinted(g, creditPanel);
		GUILabel lblCredit = new GUILabel(creditBounds, "BlueprintBot " + FBSR.getVersion(),
				GUIStyle.FONT_BP_BOLD.deriveFont(16f), Color.GRAY, GUIAlign.TOP_CENTER);
		renderTinted(g, lblCredit);

		g.setComposite(pc);
	}

	private void drawImagePane(Graphics2D g, GUIBox bounds) {
		bounds = bounds.shrink(0, 24, 24, 24);

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		renderTinted(g, panel);

		AffineTransform xform = g.getTransform();
		double renderScaleX = xform.getScaleX();
		double renderScaleY = xform.getScaleY();

		double cellOffsetX = -20 / renderScaleX;
		double cellOffsetY = -20 / renderScaleY;
		double cellWidth = (BP_CELL_SIZE.width + 20) / renderScaleX;
		double cellHeight = (BP_CELL_SIZE.height + 20) / renderScaleY;

		int centerShiftX = (int) (20 / renderScaleX);
		int centerShiftY = (int) (20 / renderScaleY);

		g.setFont(GUIStyle.FONT_BP_REGULAR.deriveFont(12f));
		g.setColor(Color.gray);
		Shape prevClip = g.getClip();

		for (ImageBlock block : blocks) {
			int x = (int) (bounds.x + bounds.width / 2
					+ (-packBounds.width / 2.0 - packBounds.x + block.location.x) * cellWidth + cellOffsetX);
			int y = (int) (bounds.y + bounds.height / 2
					+ (-packBounds.height / 2.0 - packBounds.y + block.location.y) * cellHeight + cellOffsetY);
			int w = (int) (block.location.width * cellWidth);
			int h = (int) (block.location.height * cellHeight);
			g.setClip(new Rectangle(x, y, w, h));
 
			int centerX = x + w / 2 + centerShiftX;
			int centerY = y + h / 2 + centerShiftY;

			GUIImage image = new GUIImage(new GUIBox(centerX, centerY, 0, 0), block.image, true);
			image.render(g);

			StringBuilder labelText = new StringBuilder();

			block.icons.stream().sorted(Comparator.comparing(i -> i.index)).forEach(i -> {
				TagToken tag = new TagToken(i.signal.type, i.signal.name, i.signal.quality);
				labelText.append(tag.formatted());
			});

			if (block.label.isPresent()) {
				if (labelText.length() > 0) {
					labelText.append(" ");
				}
				labelText.append(block.label.get());
			}

			if (labelText.length() > 0) {
				RichText label = new RichText(labelText.toString());
				label.draw(g, x + 25, y + 35);
			}
		}

		g.setClip(prevClip);

		Table<Integer, Integer, Integer> groupings = ArrayTable.create(
				IntStream.rangeClosed(packBounds.y, packBounds.y + packBounds.height).mapToObj(i -> (Integer) i)
						.collect(Collectors.toList()),
				IntStream.rangeClosed(packBounds.x, packBounds.x + packBounds.width).mapToObj(i -> (Integer) i)
						.collect(Collectors.toList()));
		for (int i = 0; i < blocks.size(); i++) {
			ImageBlock block = blocks.get(i);
			for (int row = block.location.y; row < block.location.y + block.location.height; row++) {
				for (int col = block.location.x; col < block.location.x + block.location.width; col++) {
					groupings.put(row, col, i);
				}
			}
		}

		int pipeX = (int) (bounds.x + bounds.width / 2 + (-packBounds.width / 2.0) * cellWidth) - 4;
		int pipeY = (int) (bounds.y + bounds.height / 2 + (-packBounds.height / 2.0) * cellHeight) - 4;
		g.setComposite(tint);
		GUIStyle.PIPE.renderDynamicGrid(g, pipeX, pipeY, cellWidth, cellHeight, packBounds, groupings);
		g.setComposite(pc);

		GUIBox boundsCell = bounds.cutTop(28).cutRight(100);

		Font fontMod = GUIStyle.FONT_BP_BOLD.deriveFont(15f);

		if (spaceAge) {
			GUIStyle.CIRCLE_WHITE.render(g, boundsCell);
			GUILabel label = new GUILabel(boundsCell, "Space Age", fontMod, Color.black, GUIAlign.CENTER);
			label.render(g);
			boundsCell = boundsCell.indexed(1, 0);
		}

		for (String mod : mods) {
			FontMetrics fm = g.getFontMetrics(fontMod);
			int minWidth = fm.stringWidth(mod) + 16;
			GUIBox boundsLabel = (minWidth > boundsCell.width) ? boundsCell.expandLeft(minWidth - boundsCell.width)
					: boundsCell;
			GUIStyle.CIRCLE_YELLOW.render(g, boundsLabel);
			GUILabel label = new GUILabel(boundsLabel, mod, fontMod, Color.black, GUIAlign.CENTER);
			label.render(g);
			boundsCell = boundsCell.indexed(1, 0);
		}

	}

	private void drawTitleBar(Graphics2D g, GUIBox bounds) {
		GUIRichText lblTitle = new GUIRichText(bounds.shrinkBottom(6).shrinkLeft(24),
				book.label.orElse("Untitled Blueprint Book"), GUIStyle.FONT_BP_BOLD.deriveFont(24f),
				GUIStyle.FONT_BP_COLOR, GUIAlign.CENTER_LEFT);
		lblTitle.render(g);

		StringBuilder iconText = new StringBuilder();
		book.icons.ifPresent(l -> l.stream().sorted(Comparator.comparing(i -> i.index)).forEach(i -> {
			TagToken tag = new TagToken(i.signal.type, i.signal.name, i.signal.quality);
			iconText.append(tag.formatted());
		}));
		GUIRichText lblIcons = new GUIRichText(bounds.shrinkBottom(6).shrinkRight(22),
				iconText.toString(), GUIStyle.FONT_BP_BOLD.deriveFont(24f), GUIStyle.FONT_BP_COLOR, GUIAlign.CENTER_RIGHT);
		lblIcons.render(g);

		int startX = bounds.x + (int) (lblTitle.getTextWidth(g) + 44);
		int endX = bounds.x + bounds.width - (int)lblIcons.getTextWidth(g) - (iconText.length() == 0 ? 24 : 46);
		GUIPipeFeature pipe = GUIStyle.DRAG_LINES;
		g.setComposite(tint);
		for (int x = endX - pipe.size; x >= startX; x -= pipe.size) {
			pipe.renderVertical(g, x, bounds.y + 10, bounds.y + bounds.height - 10);
		}
		g.setComposite(pc);
	}

	public BufferedImage generateDiscordImage() {

		double renderScale = 0.5;
		double uiScale = 1;

		// Render Images
		blocks = new ArrayList<>();
		results = new ArrayList<>();
		List<Future<RenderResult>> futures = new ArrayList<>();
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

			// TODO fix race conditions, turned off parallel for now
			Future<RenderResult> future = FBSR.renderBlueprintAsync(request);
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			futures.add(future);
		}
		for (Future<RenderResult> future : futures) {
			RenderResult result;
			try {
				result = future.get();
			} catch (InterruptedException | ExecutionException e) {
				reporting.addException(e);
				continue;
			}
			results.add(result);

			int rows = (result.image.getHeight() + BP_CELL_SIZE.height - 1) / BP_CELL_SIZE.height;
			int cols = (result.image.getWidth() + BP_CELL_SIZE.width - 1) / BP_CELL_SIZE.width;
			BSBlueprint blueprint = result.request.getBlueprint();
			blocks.add(new ImageBlock(rows, cols, blueprint.label, blueprint.icons, result.image));
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

			pc = g.getComposite();
			Set<String> groups = new LinkedHashSet<>();
			for (BSBlueprint blueprint : book.getAllBlueprints()) {
				blueprint.entities.stream().map(e -> FactorioManager.lookupEntityFactoryForName(e.name))
						.map(e -> e.isUnknown() ? "Modded" : e.getGroupName()).forEach(groups::add);
				blueprint.tiles.stream().map(t -> FactorioManager.lookupTileFactoryForName(t.name)).filter(t -> !t.isUnknown())
						.map(t -> t.getGroupName()).forEach(groups::add);
			}

			spaceAge = groups.contains("Space Age");
			groups.removeAll(Arrays.asList("Base", "Space Age"));
			mods = groups.stream().sorted().collect(Collectors.toList());

			if (!mods.isEmpty()) {
				tint = new TintComposite(450, 300, 80, 255);
			} else if (spaceAge) {
				tint = new TintComposite(350, 350, 400, 255);
			} else {
				tint = pc;
			}

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

	private void renderTinted(Graphics2D g, GUIPart part) {
		g.setComposite(tint);
		part.render(g);
		g.setComposite(pc);
	}

}
