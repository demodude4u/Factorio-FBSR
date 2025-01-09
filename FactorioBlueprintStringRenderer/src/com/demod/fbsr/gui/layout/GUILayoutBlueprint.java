package com.demod.fbsr.gui.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.TotalRawCalculator;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUISpacing;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.part.GUIImage;
import com.demod.fbsr.gui.part.GUILabel;
import com.demod.fbsr.gui.part.GUILabel.Align;
import com.demod.fbsr.gui.part.GUIPanel;

public class GUILayoutBlueprint {

	// Discord messages at 100% scale embed images at 550x350
	// This is double so it has a nice zoom but also crisp in detail
	// This is also doubled again with the scale setting (4x total)
	public static final GUISize DISCORD_IMAGE_SIZE = new GUISize(1100, 700);

	// TODO use the prototype sprite instead
	private static BufferedImage timeIcon = null;
	static {
		try {
			timeIcon = ImageIO.read(FBSR.class.getClassLoader().getResourceAsStream("Time_icon.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BSBlueprint blueprint;
	private CommandReporting reporting;
	private RenderResult result;

	private void drawFrame(Graphics2D g, GUIBox bounds) {
		int titleHeight = 50;
		// TODO dynamically widen for another column if the total items (comp +
		// crafting) exceeds number
		int infoPaneWidth = 196 + 40;
		int creditHeight = 25;

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER);
		panel.render(g);

		drawTitleBar(g, bounds.cutTop(titleHeight));
		drawInfoPane(g, bounds.shrinkTop(titleHeight).cutLeft(infoPaneWidth).shrinkBottom(creditHeight));
		drawImagePane(g, bounds.shrinkTop(titleHeight).shrinkLeft(infoPaneWidth));

		// TODO Icon

		GUILabel lblCredit = new GUILabel(
				bounds.cutLeft(infoPaneWidth).cutBottom(creditHeight).expandTop(8).shrink(0, 12, 8, 0),
				"BlueprintBot " + FBSR.getVersion(), GUIStyle.FONT_BP_BOLD.deriveFont(16f), Color.GRAY, Align.CENTER);
		lblCredit.render(g);
	}

	private void drawImagePane(Graphics2D g, GUIBox bounds) {
		bounds = bounds.shrink(0, 12, 24, 24);

		// TODO description bar along top

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		panel.render(g);

		AffineTransform xform = g.getTransform();
		int renderWidth = (int) (bounds.width * xform.getScaleX());
		int renderHeight = (int) (bounds.height * xform.getScaleY());

		RenderRequest request = new RenderRequest(blueprint, reporting);
		request.setMinWidth(OptionalInt.of(renderWidth));
		request.setMinHeight(OptionalInt.of(renderHeight));
		request.setMaxWidth(OptionalInt.of(renderWidth));
		request.setMaxHeight(OptionalInt.of(renderHeight));
		request.setBackground(Optional.empty());
		request.setGridLines(Optional.empty());

		this.result = FBSR.renderBlueprint(request);

		GUIImage image = new GUIImage(bounds, result.image, true);
		image.render(g);
	}

	private void drawInfoPane(Graphics2D g, GUIBox bounds) {
		GUISpacing subPanelInset = new GUISpacing(40, 12, 12, 12);
		GUISpacing itemGridInset = new GUISpacing(8, 8, 8, 8);
		GUISize itemGridCell = new GUISize(GUIStyle.ITEM_SLOT.source.width / 2, GUIStyle.ITEM_SLOT.source.height / 2);

		bounds = bounds.shrink(0, 24, 12, 12);

		DataTable table = FactorioData.getTable();

		GUIPanel backPanel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		backPanel.render(g);

		abstract class SubPanel {
			GUIBox bounds;
			String title;

			public SubPanel(GUIBox bounds, String title) {
				this.bounds = bounds;
				this.title = title;
			}

			void render(Graphics2D g) {
				GUIStyle.PIPE.renderBox(g, bounds.shrink(6, 6, 6, 6));

				GUILabel lblTitle = new GUILabel(bounds.cutTop(subPanelInset.top).shrink(20, 24, 8, 24), title,
						GUIStyle.FONT_BP_BOLD.deriveFont(18f), GUIStyle.FONT_BP_COLOR, Align.CENTER);
				lblTitle.render(g);
			}
		}

		List<SubPanel> subPanels = new ArrayList<>();
		int cutY = 0;

		Map<String, Double> totalItems = FBSR.generateTotalItems(blueprint);

		// Components
		{
			int itemCols = (bounds.width - subPanelInset.add(itemGridInset).getHorizontal()) / itemGridCell.width;
			int itemRows = (totalItems.size() + itemCols - 1) / itemCols;
			int subPanelHeight = subPanelInset.add(itemGridInset).getVertical() + itemGridCell.height * itemRows;
			subPanels.add(new SubPanel(bounds.shrinkTop(cutY).cutTop(subPanelHeight), "Components") {
				@Override
				void render(Graphics2D g) {
					super.render(g);

					GUIBox itemGridBounds = bounds.shrink(subPanelInset).shrink(itemGridInset);
					GUIPanel itemGridPanel = new GUIPanel(itemGridBounds, GUIStyle.FRAME_DARK_INNER,
							GUIStyle.FRAME_LIGHT_OUTER);
					itemGridPanel.render(g);

					for (int row = 0; row < itemRows; row++) {
						for (int col = 0; col < itemCols; col++) {
							GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row,
									col);
							GUIStyle.FRAME_DARK_BUMP_OUTER.render(g, cellBounds.shrink(10, 10, 10, 10));
						}
					}

					List<Entry<String, Double>> itemOrder = totalItems.entrySet().stream()
							.sorted(Comparator.comparing((Entry<String, Double> e) -> e.getValue()).reversed())
							.collect(Collectors.toList());

					for (int i = 0; i < itemOrder.size(); i++) {
						Entry<String, Double> entry = itemOrder.get(i);
						String item = entry.getKey();
						double quantity = entry.getValue();
						int col = i % itemCols;
						int row = i / itemCols;
						GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row, col);

						GUIStyle.ITEM_SLOT.render(g, cellBounds);

						Optional<ItemPrototype> protoItem = table.getItem(item);

						if (protoItem.isPresent()) {
							GUIImage imgIcon = new GUIImage(cellBounds, FactorioData.getIcon(protoItem.get()), true);
							imgIcon.render(g);
						} else {
							g.setColor(EntityRendererFactory.getUnknownColor(item));
							g.fillOval(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height);
						}

						String fmtQty = RenderUtils.fmtItemQuantity(quantity);
						g.setFont(GUIStyle.FONT_BP_BOLD.deriveFont(12f));
						int strW = g.getFontMetrics().stringWidth(fmtQty);
						int x = cellBounds.x + cellBounds.width - strW - 5;
						int y = cellBounds.y + cellBounds.height - 5;
						g.setColor(new Color(0, 0, 0, 128));
						g.drawString(fmtQty, x - 1, y - 1);
						g.drawString(fmtQty, x + 1, y + 1);
						g.setColor(Color.white);
						g.drawString(fmtQty, x, y);
					}
				}
			});
			cutY += subPanelHeight;
		}

		// Raw
		{
			Map<String, Double> totalRawItems = FBSR.generateTotalRawItems(totalItems);

			int itemCols = (bounds.width - subPanelInset.add(itemGridInset).getHorizontal()) / itemGridCell.width;
			int itemRows = (totalRawItems.size() + itemCols - 1) / itemCols;
			int subPanelHeight = subPanelInset.add(itemGridInset).getVertical() + itemGridCell.height * itemRows;
			subPanels.add(new SubPanel(bounds.shrinkTop(cutY).cutTop(subPanelHeight), "Crafting") {
				@Override
				void render(Graphics2D g) {
					super.render(g);

					GUIBox itemGridBounds = bounds.shrink(subPanelInset).shrink(itemGridInset);
					GUIPanel itemGridPanel = new GUIPanel(itemGridBounds, GUIStyle.FRAME_DARK_INNER,
							GUIStyle.FRAME_LIGHT_OUTER);
					itemGridPanel.render(g);

					for (int row = 0; row < itemRows; row++) {
						for (int col = 0; col < itemCols; col++) {
							GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row,
									col);
							GUIStyle.FRAME_DARK_BUMP_OUTER.render(g, cellBounds.shrink(10, 10, 10, 10));
						}
					}

					List<Entry<String, Double>> itemOrder = totalRawItems.entrySet().stream()
							.sorted(Comparator.comparing((Entry<String, Double> e) -> e.getValue()).reversed())
							.collect(Collectors.toList());

					for (int i = 0; i < itemOrder.size(); i++) {
						Entry<String, Double> entry = itemOrder.get(i);
						String item = entry.getKey();
						double quantity = entry.getValue();
						int col = i % itemCols;
						int row = i / itemCols;
						GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row, col);

						GUIStyle.ITEM_SLOT.render(g, cellBounds);

						Optional<BufferedImage> image;
						if (item.equals(TotalRawCalculator.RAW_TIME)) {
							image = Optional.of(timeIcon);
						} else {
							Optional<? extends DataPrototype> prototype = table.getItem(item);
							if (!prototype.isPresent()) {
								prototype = table.getFluid(item);
							}
							image = prototype.map(FactorioData::getIcon);
						}

						if (image.isPresent()) {
							GUIImage imgIcon = new GUIImage(cellBounds, image.get(), true);
							imgIcon.render(g);
						} else {
							g.setColor(EntityRendererFactory.getUnknownColor(item));
							g.fillOval(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height);
						}

						String fmtQty = RenderUtils.fmtItemQuantity(quantity);
						g.setFont(GUIStyle.FONT_BP_BOLD.deriveFont(12f));
						int strW = g.getFontMetrics().stringWidth(fmtQty);
						int x = cellBounds.x + cellBounds.width - strW - 5;
						int y = cellBounds.y + cellBounds.height - 5;
						g.setColor(new Color(0, 0, 0, 128));
						g.drawString(fmtQty, x - 1, y - 1);
						g.drawString(fmtQty, x + 1, y + 1);
						g.setColor(Color.white);
						g.drawString(fmtQty, x, y);
					}
				}
			});
			cutY += subPanelHeight;
		}

		// Grid Settings
		{

		}

		GUIPanel frontPanel = new GUIPanel(bounds.cutTop(cutY), GUIStyle.FRAME_LIGHT_INNER);
		frontPanel.render(g);

		subPanels.forEach(p -> p.render(g));
	}

	private void drawTitleBar(Graphics2D g, GUIBox bounds) {
		GUILabel lblTitle = new GUILabel(bounds.shrinkBottom(8).shrinkLeft(24),
				blueprint.label.orElse("Untitled Blueprint"), GUIStyle.FONT_BP_BOLD.deriveFont(24f),
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

		double scale = 2;

		int imageWidth = (int) (DISCORD_IMAGE_SIZE.width * scale);
		int imageHeight = (int) (DISCORD_IMAGE_SIZE.height * scale);
		BufferedImage ret = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		GUIBox bounds = new GUIBox(0, 0, (int) (ret.getWidth() / scale), (int) (ret.getHeight() / scale));

		Graphics2D g = ret.createGraphics();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.scale(scale, scale);

			drawFrame(g, bounds);
		} finally {
			g.dispose();
		}

		return ret;

	}

	public RenderResult getResult() {
		return result;
	}

	public void setBlueprint(BSBlueprint blueprint) {
		this.blueprint = blueprint;
	}

	public void setReporting(CommandReporting reporting) {
		this.reporting = reporting;
	}

}
