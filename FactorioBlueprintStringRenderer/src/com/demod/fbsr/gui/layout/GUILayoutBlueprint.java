package com.demod.fbsr.gui.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.OptionalInt;

import com.demod.dcba.CommandReporting;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.GUISize;
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

	private BSBlueprint blueprint;
	private CommandReporting reporting;
	private RenderResult result;

	private void drawFrame(Graphics2D g, GUIBox bounds) {
		int titleHeight = 50;
		int infoPaneWidth = 200;
		int creditHeight = 25;

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER);
		panel.render(g);

		drawTitleBar(g, bounds.cutTop(titleHeight));
		drawInfoPane(g, bounds.shrinkTop(titleHeight).cutLeft(infoPaneWidth).shrinkBottom(creditHeight));
		drawImagePane(g, bounds.shrinkTop(titleHeight).shrinkLeft(infoPaneWidth));

		GUILabel lblCredit = new GUILabel(
				bounds.cutLeft(infoPaneWidth).cutBottom(creditHeight).expandTop(8).shrink(0, 12, 8, 0),
				"BlueprintBot " + FBSR.getVersion(), GUIStyle.FONT_BP_BOLD.deriveFont(16f), Color.GRAY, Align.CENTER);
		lblCredit.render(g);
	}

	private void drawImagePane(Graphics2D g, GUIBox bounds) {
		bounds = bounds.shrink(0, 12, 24, 24);

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

		bounds = bounds.shrink(0, 24, 12, 12);

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER);
		panel.render(g);

		// TODO all of the info
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
