package com.demod.fbsr.gui.layout;

import java.awt.Graphics2D;
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
import com.demod.fbsr.gui.part.GUIImage;
import com.demod.fbsr.gui.part.GUILabel;
import com.demod.fbsr.gui.part.GUIPanel;

public class GUILayoutBlueprint {

	// Discord messages at 100% scale embed images at 550x350
	// This is double so it has a nice zoom but also crisp in detail
	public static final GUISize DISCORD_IMAGE_SIZE = new GUISize(1100, 700);

	private BSBlueprint blueprint;
	private CommandReporting reporting;
	private RenderResult result;

	private void drawFrame(Graphics2D g, GUIBox bounds) {
		int titleHeight = 40;
		int infoPaneWidth = 300;
		int creditHeight = 50;

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_DARK_OUTER);
		panel.render(g);
		GUILabel lblTitle = new GUILabel(bounds.cutTop(titleHeight).shrink(25, 25, 0, 0),
				blueprint.label.orElse("Blueprint"), GUIStyle.FONT_BP_BOLD.deriveFont(24f), GUIStyle.FONT_BP_COLOR);
		lblTitle.render(g);
		GUILabel lblCredit = new GUILabel(bounds.cutLeft(infoPaneWidth).cutBottom(creditHeight).shrink(25, 15, 0, 0),
				"BlueprintBot " + FBSR.getVersion(), GUIStyle.FONT_BP_BOLD.deriveFont(16f), GUIStyle.FONT_BP_COLOR);
		lblCredit.render(g);

		drawInfoPane(g, bounds.shrinkTop(titleHeight).cutLeft(infoPaneWidth).shrinkBottom(creditHeight));
		drawImagePane(g, bounds.shrinkTop(titleHeight).shrinkLeft(infoPaneWidth));
	}

	private void drawImagePane(Graphics2D g, GUIBox bounds) {
		bounds = bounds.shrink(0, 24, 12, 24);

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER, GUIStyle.FRAME_DARK_OUTER);
		panel.render(g);

		RenderRequest request = new RenderRequest(blueprint, reporting);
		request.setMinWidth(OptionalInt.of(bounds.width));
		request.setMinHeight(OptionalInt.of(bounds.height));
		request.setMaxWidth(OptionalInt.of(bounds.width));
		request.setMaxHeight(OptionalInt.of(bounds.height));
		request.setBackground(Optional.empty());

		this.result = FBSR.renderBlueprint(request);

		GUIImage image = new GUIImage(bounds, result.image);
		image.render(g);
	}

	private void drawInfoPane(Graphics2D g, GUIBox bounds) {

		bounds = bounds.shrink(0, 12, 24, 24);

		GUIPanel panel = new GUIPanel(bounds, GUIStyle.FRAME_INNER, GUIStyle.FRAME_DARK_OUTER);
		panel.render(g);

		// TODO all of the info
	}

	public BufferedImage generateDiscordImage() {

		BufferedImage ret = new BufferedImage(DISCORD_IMAGE_SIZE.width, DISCORD_IMAGE_SIZE.height,
				BufferedImage.TYPE_INT_ARGB);

		GUIBox bounds = new GUIBox(0, 0, ret.getWidth(), ret.getHeight());

		Graphics2D g = ret.createGraphics();
		try {
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
