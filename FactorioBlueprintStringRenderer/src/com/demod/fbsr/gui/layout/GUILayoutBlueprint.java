package com.demod.fbsr.gui.layout;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.DataTable;
import com.demod.factorio.TotalRawCalculator;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.RichText.TagToken;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.composite.TintComposite;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.GUIAlign;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISize;
import com.demod.fbsr.gui.GUISpacing;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.part.GUIImage;
import com.demod.fbsr.gui.part.GUIImageDef;
import com.demod.fbsr.gui.part.GUILabel;
import com.demod.fbsr.gui.part.GUIPanel;
import com.demod.fbsr.gui.part.GUIPart;
import com.demod.fbsr.gui.part.GUIRichText;
import com.google.common.collect.ImmutableMap;

public class GUILayoutBlueprint {

	// Discord messages at 100% scale embed images at 550x350
	// This is double so it has a nice zoom but also crisp in detail
	public static final GUISize DISCORD_IMAGE_SIZE = new GUISize(1100, 700);

	private BSBlueprint blueprint;
	private CommandReporting reporting;
	private RenderResult result;

	private Map<String, Double> totalItems;
	private Map<String, Double> totalRawItems;

	private int itemColumns;
	private int itemCellSize;
	private float itemFontSize;
	private int itemFontOffset;
	private boolean itemShowCellBackground;

	private boolean spaceAge;

	private List<String> mods;

	private Composite pc;
	private Composite tint;

	private Graphics2D g;

	private void drawFrame(GUIBox bounds) {
		int titleHeight = 50;
		int infoPaneWidth = 76 + itemColumns * itemCellSize;

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER);
		renderTinted(panel);

		drawTitleBar(bounds.cutTop(titleHeight));
		drawInfoPane(bounds.shrinkTop(titleHeight).cutLeft(infoPaneWidth));
		drawImagePane(bounds.shrinkTop(titleHeight).shrinkLeft(infoPaneWidth));

		GUIBox creditBounds = bounds.cutRight(190).cutBottom(24).expandTop(8).cutTop(16).cutLeft(160);
		GUIPanel creditPanel = new GUIPanel(creditBounds, GUIStyle.FRAME_TAB);
		renderTinted(creditPanel);
		GUILabel lblCredit = new GUILabel(creditBounds, "BlueprintBot " + FBSR.getVersion(),
				GUIStyle.FONT_BP_BOLD.deriveFont(16f), Color.GRAY, GUIAlign.TOP_CENTER);
		renderTinted(lblCredit);
	}

	private void drawImagePane(GUIBox bounds) {
		bounds = bounds.shrink(0, 12, 24, 24);

		// TODO description bar along top

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		renderTinted(panel);

		boolean foundation = blueprint.tiles.stream().anyMatch(t -> {
			Optional<TilePrototype> tile = FactorioManager.lookupTileByName(t.name);
			return tile.isPresent() && tile.get().isFoundation();
		});
		if (foundation) {
			g.setColor(new Color(0, 0, 2, 220));
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}

		AffineTransform xform = g.getTransform();
		int renderWidth = (int) (bounds.width * xform.getScaleX());
		int renderHeight = (int) (bounds.height * xform.getScaleY());

		RenderRequest request = new RenderRequest(blueprint, reporting);
		request.setMinWidth(OptionalInt.of(renderWidth));
		request.setMinHeight(OptionalInt.of(renderHeight));
		request.setMaxWidth(OptionalInt.of(renderWidth));
		request.setMaxHeight(OptionalInt.of(renderHeight));
		request.setMaxScale(OptionalDouble.of(2.0));
		request.setBackground(Optional.empty());
		request.setDontClipSprites(false);

		this.result = FBSR.renderBlueprint(request);

		GUIImage image = new GUIImage(bounds, result.image, true);
		image.render(g);

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

	private void drawInfoPane(GUIBox bounds) {
		GUISpacing subPanelInset = new GUISpacing(40, 12, 12, 12);
		GUISpacing itemGridInset = new GUISpacing(8, 8, 8, 8);
		GUISize itemGridCell = new GUISize(itemCellSize, itemCellSize);

		bounds = bounds.shrink(0, 24, 24, 12);

		GUIPanel backPanel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		renderTinted(backPanel);

		Font fontQty = GUIStyle.FONT_BP_BOLD.deriveFont(itemFontSize);

		abstract class SubPanel {
			GUIBox bounds;
			String title;

			public SubPanel(GUIBox bounds, String title) {
				this.bounds = bounds;
				this.title = title;
			}

			void render(Graphics2D g) {
				g.setComposite(tint);
				GUIStyle.PIPE.renderBox(g, bounds.shrink(6, 6, 6, 6));
				g.setComposite(pc);

				GUILabel lblTitle = new GUILabel(bounds.cutTop(subPanelInset.top).shrink(20, 24, 8, 24), title,
						GUIStyle.FONT_BP_BOLD.deriveFont(18f), GUIStyle.FONT_BP_COLOR, GUIAlign.CENTER);
				lblTitle.render(g);
			}
		}

		List<SubPanel> subPanels = new ArrayList<>();
		int cutY = 0;

		// Items
		{
			int itemRows = (totalItems.size() + itemColumns - 1) / itemColumns;
			int subPanelHeight = subPanelInset.add(itemGridInset).getVertical() + itemGridCell.height * itemRows;
			subPanels.add(new SubPanel(bounds.shrinkTop(cutY).cutTop(subPanelHeight), "Items") {
				@Override
				void render(Graphics2D g) {
					super.render(g);

					GUIBox itemGridBounds = bounds.shrink(subPanelInset).shrink(itemGridInset);
					GUIPanel itemGridPanel = new GUIPanel(itemGridBounds, GUIStyle.FRAME_DARK_INNER,
							GUIStyle.FRAME_LIGHT_OUTER);
					renderTinted(itemGridPanel);

					if (itemShowCellBackground) {
						for (int row = 0; row < itemRows; row++) {
							for (int col = 0; col < itemColumns; col++) {
								GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row,
										col);
								int bump = itemCellSize / 4;
								GUIStyle.FRAME_DARK_BUMP_OUTER.render(g, cellBounds.shrink(bump, bump, bump, bump));
							}
						}
					}

					List<Entry<String, Double>> itemOrder = totalItems.entrySet().stream()
							.sorted(Comparator.comparing((Entry<String, Double> e) -> e.getValue()).reversed())
							.collect(Collectors.toList());

					for (int i = 0; i < itemOrder.size(); i++) {
						Entry<String, Double> entry = itemOrder.get(i);
						String item = entry.getKey();
						double quantity = entry.getValue();
						int col = i % itemColumns;
						int row = i / itemColumns;
						GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row, col);

						GUIStyle.ITEM_SLOT.render(g, cellBounds);

						int iconShrink = (int) (itemCellSize * 0.15);
						GUIBox iconBounds = cellBounds.shrink(iconShrink, iconShrink, iconShrink, iconShrink);

						Optional<IconDef> icon = IconManager.lookupItem(item);
						if (icon.isPresent()) {
							GUIImageDef imgIcon = new GUIImageDef(iconBounds, icon.get());
							imgIcon.render(g);
						} else {
							g.setColor(RenderUtils.getUnknownColor(item).brighter());
							g.fillOval(iconBounds.x, iconBounds.y, iconBounds.width, iconBounds.height);
						}

						String fmtQty = RenderUtils.fmtItemQuantity(quantity);
						g.setFont(fontQty);
						int strW = g.getFontMetrics().stringWidth(fmtQty);
						int x = cellBounds.x + cellBounds.width - strW - itemFontOffset;
						int y = cellBounds.y + cellBounds.height - itemFontOffset;
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
		if (totalRawItems.size() > 0) {
			int itemRows = (totalRawItems.size() + itemColumns - 1) / itemColumns;
			int subPanelHeight = subPanelInset.add(itemGridInset).getVertical() + itemGridCell.height * itemRows;
			subPanels.add(new SubPanel(bounds.shrinkTop(cutY).cutTop(subPanelHeight), "Raw") {

				@Override
				void render(Graphics2D g) {
					super.render(g);

					GUIBox itemGridBounds = bounds.shrink(subPanelInset).shrink(itemGridInset);
					GUIPanel itemGridPanel = new GUIPanel(itemGridBounds, GUIStyle.FRAME_DARK_INNER,
							GUIStyle.FRAME_LIGHT_OUTER);
					renderTinted(itemGridPanel);

					if (itemShowCellBackground) {
						for (int row = 0; row < itemRows; row++) {
							for (int col = 0; col < itemColumns; col++) {
								GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row,
										col);
								int bump = itemCellSize / 4;
								GUIStyle.FRAME_DARK_BUMP_OUTER.render(g, cellBounds.shrink(bump, bump, bump, bump));
							}
						}
					}

					List<Entry<String, Double>> itemOrder = totalRawItems.entrySet().stream()
							.sorted(Comparator.comparing((Entry<String, Double> e) -> e.getValue()).reversed())
							.collect(Collectors.toList());

					for (int i = 0; i < itemOrder.size(); i++) {
						Entry<String, Double> entry = itemOrder.get(i);
						String item = entry.getKey();
						double quantity = entry.getValue();
						int col = i % itemColumns;
						int row = i / itemColumns;
						GUIBox cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row, col);

						GUIStyle.ITEM_SLOT.render(g, cellBounds);

						int iconSize = (itemCellSize * 32) / 40;
						GUIBox iconBounds = new GUIBox(cellBounds.x + cellBounds.width / 2 - iconSize / 2,
								cellBounds.y + cellBounds.height / 2 - iconSize / 2, iconSize, iconSize);

						Optional<? extends ImageDef> image = null;
						if (item.equals(TotalRawCalculator.RAW_TIME)) {
							image = Optional.of(GUIStyle.DEF_CLOCK);
						} else {
							image = IconManager.lookupItem(item);
							if (image.isEmpty()) {
								image = IconManager.lookupFluid(item);
							}
						}

						if (image.isPresent()) {
							GUIImageDef imgIcon = new GUIImageDef(iconBounds, image.get());
							imgIcon.render(g);
						} else {
							g.setColor(RenderUtils.getUnknownColor(item));
							g.fillOval(iconBounds.x, iconBounds.y, iconBounds.width, iconBounds.height);
						}

						String fmtQty = RenderUtils.fmtItemQuantity(quantity);
						g.setFont(fontQty);
						int strW = g.getFontMetrics().stringWidth(fmtQty);
						int x = cellBounds.x + cellBounds.width - strW - itemFontOffset;
						int y = cellBounds.y + cellBounds.height - itemFontOffset;
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

		// TODO Grid Settings
		{

		}

		GUIPanel frontPanel = new GUIPanel(bounds.cutTop(cutY), GUIStyle.FRAME_LIGHT_INNER);
		renderTinted(frontPanel);

		subPanels.forEach(p -> p.render(g));
	}

	private void drawTitleBar(GUIBox bounds) {
		GUIRichText lblTitle = new GUIRichText(bounds.shrinkBottom(6).shrinkLeft(24),
				blueprint.label.orElse("Untitled Blueprint"), GUIStyle.FONT_BP_BOLD.deriveFont(24f),
				GUIStyle.FONT_BP_COLOR, GUIAlign.CENTER_LEFT);
		lblTitle.render(g);

		StringBuilder iconText = new StringBuilder();
		blueprint.icons.stream().sorted(Comparator.comparing(i -> i.index)).forEach(i -> {
			TagToken tag = new TagToken(i.signal.type, i.signal.name, i.signal.quality);
			iconText.append(tag.formatted());
		});
		GUIRichText lblIcons = new GUIRichText(bounds.shrinkBottom(6).shrinkRight(22),
				iconText.toString(), GUIStyle.FONT_BP_BOLD.deriveFont(24f), GUIStyle.FONT_BP_COLOR, GUIAlign.CENTER_RIGHT);
		lblIcons.render(g);

		int startX = bounds.x + (int)lblTitle.getTextWidth(g) + 44;
		int endX = bounds.x + bounds.width - (int)lblIcons.getTextWidth(g) - (iconText.length() == 0 ? 24 : 46);
		GUIPipeFeature pipe = GUIStyle.DRAG_LINES;
		g.setComposite(tint);
		for (int x = endX - pipe.size; x >= startX; x -= pipe.size) {
			pipe.renderVertical(g, x, bounds.y + 10, bounds.y + bounds.height - 10);
		}
		g.setComposite(pc);
	}

	public BufferedImage generateDiscordImage() {

		DataTable baseTable = FactorioManager.getBaseData().getTable();
		boolean baseDataOnly = blueprint.entities.stream().allMatch(e -> baseTable.getEntity(e.name).isPresent())
				&& blueprint.tiles.stream().allMatch(t -> baseTable.getTile(t.name).isPresent());

		totalItems = FBSR.generateTotalItems(blueprint);
		totalRawItems = baseDataOnly ? FBSR.generateTotalRawItems(totalItems) : ImmutableMap.of();

		Set<String> groups = new LinkedHashSet<>();
		blueprint.entities.stream().map(e -> FactorioManager.lookupEntityFactoryForName(e.name))
				.map(e -> e.isUnknown() ? "Modded" : e.getGroupName()).forEach(groups::add);
		blueprint.tiles.stream().map(t -> FactorioManager.lookupTileFactoryForName(t.name)).filter(t -> !t.isUnknown())
				.map(t -> t.getGroupName()).forEach(groups::add);

		spaceAge = groups.contains("Space Age");
		groups.removeAll(Arrays.asList("Base", "Space Age"));
		mods = groups.stream().sorted().collect(Collectors.toList());

		int itemCount = totalItems.size() + totalRawItems.size();
		int itemRowMax;
		if (itemCount <= 32) {
			itemRowMax = 8;
			itemCellSize = 40;
			itemFontSize = 12f;
			itemFontOffset = 5;
			itemShowCellBackground = true;
		} else if (itemCount <= 72) {
			itemRowMax = 12;
			itemCellSize = 30;
			itemFontSize = 10f;
			itemFontOffset = 4;
			itemShowCellBackground = true;
		} else if (itemCount <= 128) {
			itemRowMax = 16;
			itemCellSize = 20;
			itemFontSize = 8f;
			itemFontOffset = 3;
			itemShowCellBackground = true;
		} else {
			itemRowMax = 32;
			itemCellSize = 10;
			itemFontSize = 4f;
			itemFontOffset = 1;
			itemShowCellBackground = false;
		}

		itemColumns = Math.max(1, (itemCount + itemRowMax - 1) / itemRowMax);

		double scale = 2;

		int imageWidth = (int) (DISCORD_IMAGE_SIZE.width * scale);
		int imageHeight = (int) (DISCORD_IMAGE_SIZE.height * scale);
		BufferedImage ret = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		GUIBox bounds = new GUIBox(0, 0, (int) (ret.getWidth() / scale), (int) (ret.getHeight() / scale));

		g = ret.createGraphics();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.scale(scale, scale);

			pc = g.getComposite();
			if (!mods.isEmpty()) {
				tint = new TintComposite(450, 300, 80, 255);
			} else if (spaceAge) {
				tint = new TintComposite(350, 350, 400, 255);
			} else {
				tint = pc;
			}

			drawFrame(bounds);
		} finally {
			g.dispose();
		}

		return ret;

	}

	private void renderTinted(GUIPart part) {
		g.setComposite(tint);
		part.render(g);
		g.setComposite(pc);
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
