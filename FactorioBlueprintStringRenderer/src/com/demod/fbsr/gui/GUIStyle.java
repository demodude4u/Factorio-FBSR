package com.demod.fbsr.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.feature.GUISliceFeature;
import com.demod.fbsr.gui.feature.GUIStaticFeature;

public final class GUIStyle {
	private static final Logger LOGGER = LoggerFactory.getLogger(GUIStyle.class);

	public static Font FONT_BP_REGULAR;
	public static Font FONT_BP_BOLD;
	public static Color FONT_BP_COLOR;

	public static GUISliceFeature FRAME_INNER;
	public static GUISliceFeature FRAME_OUTER;
	public static GUISliceFeature FRAME_DARK_INNER;
	public static GUISliceFeature FRAME_DARK_OUTER;
	public static GUISliceFeature FRAME_DARK_BUMP_OUTER;
	public static GUISliceFeature FRAME_WHITE_INNER;
	public static GUISliceFeature FRAME_WHITE_OUTER;
	public static GUISliceFeature FRAME_WHITE_DARK_INNER;
	public static GUISliceFeature FRAME_LIGHT_INNER;
	public static GUISliceFeature FRAME_LIGHT_OUTER;
	public static GUISliceFeature CIRCLE_WHITE;
	public static GUISliceFeature CIRCLE_YELLOW;
	public static GUISliceFeature FRAME_TAB;
	public static GUIPipeFeature PIPE;
	public static GUIPipeFeature DRAG_LINES;
	public static GUIPipeFeature DRAG_LINES_WHITE;
	public static GUIStaticFeature ITEM_SLOT;

	public static ImageDef DEF_CLOCK;

	public static Font createFont(String folder, String filename) {

		File folderFonts = new File(FactorioManager.getFolderDataRoot(), "fonts");
		folderFonts.mkdirs();

		File fileFont = new File(folderFonts, filename);

		try {
			if (FactorioManager.hasFactorioInstall() && !fileFont.exists()) {
				InputStream inputStream = FactorioManager.getBaseProfile().getData().getModResource(folder + "/" + filename).get();
				Files.copy(inputStream, fileFont.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			return Font.createFont(Font.TRUETYPE_FONT, fileFont);
		} catch (FontFormatException | IOException e) {
			LOGGER.error("FAILED TO LOAD FONT: {}", fileFont.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	public static void initialize() {
		ModsProfile profile = FactorioManager.getBaseProfile();
		String filename = "__core__/graphics/gui-new.png";

		FONT_BP_REGULAR = createFont("__core__/fonts", "Lilittium-Regular.ttf");
		FONT_BP_BOLD = createFont("__core__/fonts", "Lilittium-Bold.ttf");
		FONT_BP_COLOR = new Color(0xffe6c0);

		FRAME_INNER = GUISliceFeature.inner(profile, filename, new GUIBox(0, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_OUTER = GUISliceFeature.outer(profile, filename, new GUIBox(17, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_DARK_INNER = GUISliceFeature.inner(profile, filename, new GUIBox(34, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_DARK_OUTER = GUISliceFeature.outer(profile, filename, new GUIBox(51, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_DARK_BUMP_OUTER = GUISliceFeature.outer(profile, filename, new GUIBox(282, 17, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_WHITE_INNER = GUISliceFeature.inner(profile, filename, new GUIBox(446, 86, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_WHITE_OUTER = GUISliceFeature.outer(profile, filename, new GUIBox(463, 86, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_WHITE_DARK_INNER = GUISliceFeature.inner(profile, filename, new GUIBox(313, 48, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_LIGHT_INNER = GUISliceFeature.inner(profile, filename, new GUIBox(68, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
		FRAME_LIGHT_OUTER = GUISliceFeature.outer(profile, filename, new GUIBox(85, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
		CIRCLE_WHITE = GUISliceFeature.inner(profile, filename, new GUIBox(128, 96, 28, 28), new GUISpacing(14, 14, 13, 13));
		CIRCLE_YELLOW = GUISliceFeature.inner(profile, filename, new GUIBox(156, 96, 28, 28), new GUISpacing(14, 14, 13, 13));
		FRAME_TAB = GUISliceFeature.inner(profile, filename, new GUIBox(448, 103, 17, 17), new GUISpacing(16, 8, 0, 8));
		PIPE = GUIPipeFeature.full(profile, filename, new GUIBox(0, 40, 120, 8));
		DRAG_LINES = GUIPipeFeature.dragLines(profile, filename, new GUIBox(192, 8, 24, 8));
		DRAG_LINES_WHITE = GUIPipeFeature.dragLines(profile, filename, new GUIBox(446, 78, 24, 8));
		ITEM_SLOT = new GUIStaticFeature(profile, filename, new GUIBox(0, 736, 80, 80));

		DEF_CLOCK = FactorioManager.getUtilitySprites().clock.defineSprites().get(0);
		DEF_CLOCK.getProfile().getAtlasPackage().registerDef(DEF_CLOCK);
	}
}
