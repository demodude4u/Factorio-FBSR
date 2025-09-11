package com.demod.fbsr.gui.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
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
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.dcba.CommandReporting;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.RichText;
import com.demod.fbsr.RichText.TagToken;
import com.demod.fbsr.app.DiscordService;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprint.BlueprintModInfo;
import com.demod.fbsr.bs.BSBlueprintBook;
import com.demod.fbsr.bs.BSIcon;
import com.demod.fbsr.composite.TintComposite;
import com.demod.fbsr.gui.GUIAlign;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUISpacing;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.part.GUIImage;
import com.demod.fbsr.gui.part.GUILabel;
import com.demod.fbsr.gui.part.GUIPanel;
import com.demod.fbsr.gui.part.GUIPart;
import com.demod.fbsr.gui.part.GUIRichText;
import com.demod.fbsr.gui.part.GUIRichTextArea;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import com.demod.fbsr.Quadtree;

public class GUILayoutBook implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(GUILayoutBook.class);

	//Only allow one instance of GUILayoutBook at a time (memory safety)
	private static final Semaphore EXCLUSIVE_LOCK = new Semaphore(1);

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
	public static final int MAX_PIXELS = 6144 * 6144;

	private volatile boolean disposed = false;

	public GUILayoutBook() {
		EXCLUSIVE_LOCK.acquireUninterruptibly();
	}

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
	 * Attempts to place all rectangles in the given width and height using a Quadtree
	 * for fast collision detection (similar to AtlasPackage).
	 */
	private static boolean tryPlaceRectangles(List<ImageBlock> blocks, int width, int height) {
		Quadtree occupied = new Quadtree(0, new Rectangle(0, 0, width, height));

		for (int i = 0; i < blocks.size(); i++) {
			Rectangle r = blocks.get(i).location;
			boolean placed = false;

			// Allow placement flush with right/bottom edges
			placement: for (int y = 0; y <= height - r.height; y++) {
				for (int x = 0; x <= width - r.width; x++) {
					r.setLocation(x, y);

					Rectangle collision = occupied.insertIfNoCollision(new Rectangle(r));
					if (collision == null) {
						placed = true;
						break placement;
					} else {
						// Skip past the colliding rectangle without adding an extra gap
						x = Math.max(x, collision.x + collision.width - 1);
					}
				}
			}

			if (!placed) {
				return false; // could not place this rectangle
			}
		}
		return true;
	}

	private FactorioManager factorioManager = FBSR.getFactorioManager();
	private GUIStyle guiStyle = FBSR.getGuiStyle();

	private BSBlueprintBook book;
	private ModdingResolver resolver;
	private CommandReporting reporting;
	private List<RenderResult> results;
	private List<ImageBlock> blocks;
	private Rectangle packBounds;
	private Composite pc;
	private Composite tint;

	private List<String> spaceAgeMods;
	private List<String> mods;

	private volatile String lockKey = null;

	private void drawFrame(Graphics2D g, GUIBox bounds) {
		g.setComposite(tint);

		int titleHeight = 50;

		GUIPanel panel = new GUIPanel(bounds, guiStyle.FRAME_INNER);
		renderTinted(g, panel);

		drawTitleBar(g, bounds.cutTop(titleHeight));
		drawImagePane(g, bounds.shrinkTop(titleHeight));

		String versionText;
		if (!mods.isEmpty()) {
			versionText = "Modded Factorio " + book.version;
		} else if (!spaceAgeMods.isEmpty()) {
			versionText = "Factorio Space Age " + book.version;
		} else {
			versionText = "Factorio " + book.version;
		}
		Font versionFont = guiStyle.FONT_BP_BOLD.deriveFont(16f);
		FontMetrics fm = g.getFontMetrics(versionFont);
		int versionWidth = fm.stringWidth(versionText) + 24;
		GUIBox versionBounds = bounds.cutRight(versionWidth + 30).cutBottom(24).expandTop(8).cutTop(16).cutLeft(versionWidth);
		GUIPanel versionPanel = new GUIPanel(versionBounds, guiStyle.FRAME_TAB);
		renderTinted(g, versionPanel);
		GUILabel lblVersion = new GUILabel(versionBounds, versionText, versionFont, Color.GRAY, GUIAlign.TOP_CENTER);
		renderTinted(g, lblVersion);

		GUIBox creditBounds = bounds.cutLeft(90).cutBottom(24).shrinkBottom(2).shrinkLeft(24);
		String creditText = "BlueprintBot " + FBSR.getFactorioManager().getProfileVanilla().getFactorioData().getVersion();
		Font creditFont = guiStyle.FONT_BP_REGULAR.deriveFont(10f);
		GUILabel lblCredit = new GUILabel(creditBounds, creditText, creditFont, Color.black, GUIAlign.CENTER_LEFT);
		lblCredit.color = new Color(43, 41, 41);
		lblCredit.box = creditBounds.shift(-1, 0);
		renderTinted(g, lblCredit);
		lblCredit.box = creditBounds.shift(1, 0);
		renderTinted(g, lblCredit);
		lblCredit.color = new Color(30, 30, 30);
		lblCredit.box = creditBounds.shift(0, -1);
		renderTinted(g, lblCredit);
		lblCredit.color = new Color(96, 94, 94);
		lblCredit.box = creditBounds.shift(0, 1);
		renderTinted(g, lblCredit);
		lblCredit.color = new Color(51, 48, 48);
		lblCredit.box = creditBounds;
		renderTinted(g, lblCredit);

		g.setComposite(pc);
	}

	private void drawImagePane(Graphics2D g, GUIBox bounds) {
		bounds = bounds.shrink(0, 24, 24, 24);

		GUIPanel panel = new GUIPanel(bounds, guiStyle.FRAME_DARK_INNER, guiStyle.FRAME_OUTER);
		renderTinted(g, panel);

		Paint pp = g.getPaint();
		g.setPaint(new GradientPaint(
				new Point2D.Double(bounds.x, bounds.y), 
				new Color(0, 0, 0, 0), 
				new Point2D.Double(bounds.x, bounds.y + bounds.height), 
				new Color(0, 0, 0, 32)));
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		g.setPaint(pp);

		GUIBox gridBounds = bounds.shrink(10, 10, 10, 10).shift(0, -3);
		
		// Use floating-point division to avoid integer truncation
		double cellWidth = (double) gridBounds.width / (double) packBounds.width;
		double cellHeight = (double) gridBounds.height / (double) packBounds.height;

		g.setFont(guiStyle.FONT_BP_REGULAR.deriveFont(12f));
		g.setColor(Color.gray);

		AffineTransform pat = g.getTransform();

		for (ImageBlock block : blocks) {
			int x = gridBounds.x + (int) (cellWidth * block.location.x);
			int y = gridBounds.y + (int) (cellHeight * block.location.y);
			int w = gridBounds.x + (int) (cellWidth * (block.location.x + block.location.width)) - x;
			int h = gridBounds.y + (int) (cellHeight * (block.location.y + block.location.height)) - y;

			double imageScale = Math.min(1, Math.min(
					w / (double) block.image.getWidth(),
					h / (double) block.image.getHeight()));

			g.translate(x + w / 2, y + h / 2);
			g.scale(imageScale, imageScale);
			g.drawImage(block.image, -block.image.getWidth() / 2, -block.image.getHeight() / 2, null);
			g.setTransform(pat);

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
				RichText label = new RichText(labelText.toString(), resolver);
				label.draw(g, x + 7, y + 17);
			}
		}

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

		g.setComposite(tint);
		guiStyle.PIPE.renderDynamicGrid(g, gridBounds.x - 4, gridBounds.y - 4, cellWidth, cellHeight, packBounds, groupings);
		g.setComposite(pc);

		GUIBox boundsCell;
		Font fontMod;
		if (spaceAgeMods.size() + mods.size() > 4) {
			boundsCell = bounds.cutTop(20).cutRight(80);
			fontMod = guiStyle.FONT_BP_BOLD.deriveFont(10f);
		} else {
			boundsCell = bounds.cutTop(28).cutRight(100);
			fontMod = guiStyle.FONT_BP_BOLD.deriveFont(15f);
		}

		FontMetrics fm = g.getFontMetrics(fontMod);
		for (String mod : spaceAgeMods) {
			int minWidth = fm.stringWidth(mod) + 16;
			GUIBox boundsLabel = (minWidth > boundsCell.width) ? boundsCell.expandLeft(minWidth - boundsCell.width)
					: boundsCell;
			guiStyle.CIRCLE_WHITE.render(g, boundsLabel);
			GUILabel label = new GUILabel(boundsLabel, mod, fontMod, Color.black, GUIAlign.CENTER);
			label.render(g);
			boundsCell = boundsCell.indexed(1, 0);
		}
		for (String mod : mods) {
			int minWidth = fm.stringWidth(mod) + 16;
			GUIBox boundsLabel = (minWidth > boundsCell.width) ? boundsCell.expandLeft(minWidth - boundsCell.width)
					: boundsCell;
			guiStyle.CIRCLE_YELLOW.render(g, boundsLabel);
			GUILabel label = new GUILabel(boundsLabel, mod, fontMod, Color.black, GUIAlign.CENTER);
			label.render(g);
			boundsCell = boundsCell.indexed(1, 0);
		}
		
		String description = book.description.orElse("").trim();
		if (!description.isBlank()) {
			Font fontDesc = guiStyle.FONT_BP_REGULAR.deriveFont(12f);
			FontMetrics fmDesc = g.getFontMetrics(fontDesc);
			int ascent = fmDesc.getAscent();
			int descent = fmDesc.getDescent();
			int lineHeight = ascent + descent;
			GUIBox boundsDescPanel = bounds.cutBottom(lineHeight * 20 + 8).maxWidth(750);
			GUIBox boundsDesc = boundsDescPanel.shrink(0, 6, 0, 6);
			GUIRichTextArea textArea = new GUIRichTextArea(boundsDesc, description, guiStyle.FONT_BP_REGULAR.deriveFont(12f), Color.gray, GUIAlign.CENTER_LEFT, resolver);
			int lineCount = textArea.getLineCount(g);
			if (lineCount < 20) {
				boundsDescPanel = bounds.cutBottom(lineHeight * lineCount + 8).maxWidth(750);
				boundsDesc = boundsDescPanel.shrink(0, 6, 0, 6);
				textArea.box = boundsDesc;
			}
			GUIPanel panelDesc = new GUIPanel(boundsDescPanel, guiStyle.CIRCLE_TRANSLUCENT_BLACK);
			renderTinted(g, panelDesc);
			textArea.render(g);
		}
	}

	private void drawTitleBar(Graphics2D g, GUIBox bounds) {
		GUIRichText lblTitle = new GUIRichText(bounds.shrinkBottom(6).shrinkLeft(24),
				book.label.orElse("Untitled Blueprint Book"), guiStyle.FONT_BP_BOLD.deriveFont(24f),
				guiStyle.FONT_BP_COLOR, GUIAlign.CENTER_LEFT, resolver);
		lblTitle.render(g);

		StringBuilder iconText = new StringBuilder();
		book.icons.ifPresent(l -> l.stream().sorted(Comparator.comparing(i -> i.index)).forEach(i -> {
			TagToken tag = new TagToken(i.signal.type, i.signal.name, i.signal.quality);
			iconText.append(tag.formatted());
		}));
		GUIRichText lblIcons = new GUIRichText(bounds.shrinkBottom(6).shrinkRight(22),
				iconText.toString(), guiStyle.FONT_BP_BOLD.deriveFont(24f), guiStyle.FONT_BP_COLOR, GUIAlign.CENTER_RIGHT, resolver);
		lblIcons.render(g);

		int startX = bounds.x + (int) (lblTitle.getTextWidth(g) + 44);
		int endX = bounds.x + bounds.width - (int)lblIcons.getTextWidth(g) - (iconText.length() == 0 ? 24 : 46);
		GUIPipeFeature pipe = guiStyle.DRAG_LINES;
		g.setComposite(tint);
		for (int x = endX - pipe.size; x >= startX; x -= pipe.size) {
			pipe.renderVertical(g, x, bounds.y + 10, bounds.y + bounds.height - 10);
		}
		g.setComposite(pc);
	}

	public BufferedImage generateDiscordImage() {
		double renderScale = 0.5;

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

			Future<RenderResult> future = FBSR.renderBlueprintQueued(request, lockKey);
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

		LOGGER.info("Compiling blueprint book image...");

		packBounds = packBlocks(blocks, DISCORD_IMAGE_RATIO);

		int imageWidth = BP_CELL_SIZE.width * packBounds.width;
		int imageHeight = BP_CELL_SIZE.height * packBounds.height;
		
		// Pipe gaps between cells
		imageWidth += (packBounds.width + 1) * 20;
		imageHeight += (packBounds.height + 1) * 20;
		// Framing
		imageWidth += 48;
		imageHeight += 78;

		if ((imageWidth * imageHeight) > MAX_PIXELS) {
			double shrinkFactor = Math.sqrt((double) (imageWidth * imageHeight) / (double) MAX_PIXELS);
			imageWidth = (int)(imageWidth / shrinkFactor);
			imageHeight = (int)(imageHeight / shrinkFactor);
		}
		BufferedImage ret = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		GUIBox bounds = new GUIBox(0, 0, ret.getWidth(), ret.getHeight());

		Graphics2D g = ret.createGraphics();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			pc = g.getComposite();
			Set<String> mods = new LinkedHashSet<>();
			Set<String> spaceAgeMods = new LinkedHashSet<>();
			for (BSBlueprint blueprint : book.getAllBlueprints()) {
				BlueprintModInfo modInfo = blueprint.loadModInfo(resolver);
				mods.addAll(modInfo.mods);
				spaceAgeMods.addAll(modInfo.spaceAgeMods);
			}
			this.spaceAgeMods = spaceAgeMods.stream().sorted().collect(Collectors.toList());
			this.mods = mods.stream().sorted().collect(Collectors.toList());

			if (!mods.isEmpty()) {
				tint = new TintComposite(450, 300, 80, 255);
			} else if (!spaceAgeMods.isEmpty()) {
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
		this.resolver = ModdingResolver.byBlueprintBiases(factorioManager, book);
	}

	public void setReporting(CommandReporting reporting) {
		this.reporting = reporting;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	private void renderTinted(Graphics2D g, GUIPart part) {
		g.setComposite(tint);
		part.render(g);
		g.setComposite(pc);
	}

	@Override
	public synchronized void close() throws Exception {
		if (!disposed) {
			disposed = true;
			EXCLUSIVE_LOCK.release();
		}
	}
}
