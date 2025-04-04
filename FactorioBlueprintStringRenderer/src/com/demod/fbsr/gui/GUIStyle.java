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

import com.demod.fbsr.AtlasManager;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.feature.GUISliceFeature;
import com.demod.fbsr.gui.feature.GUIStaticFeature;

public final class GUIStyle {
	private static final Logger LOGGER = LoggerFactory.getLogger(GUIStyle.class);

	public static final Font FONT_BP_REGULAR = createFont("__core__/fonts", "Lilittium-Regular.ttf");
	public static final Font FONT_BP_BOLD = createFont("__core__/fonts", "Lilittium-Bold.ttf");
	public static final Color FONT_BP_COLOR = new Color(0xffe6c0);

	// TODO load these details from data.raw.gui-styles.default

	public static final String DEFAULT_TILESET = "__core__/graphics/gui-new.png";

	public static final GUISliceFeature FRAME_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(0, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static final GUISliceFeature FRAME_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(17, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static final GUISliceFeature FRAME_DARK_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(34, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static final GUISliceFeature FRAME_DARK_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(51, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static final GUISliceFeature FRAME_DARK_BUMP_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(282, 17, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static final GUISliceFeature FRAME_WHITE_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(446, 86, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static final GUISliceFeature FRAME_WHITE_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(463, 86, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static final GUISliceFeature FRAME_WHITE_DARK_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(313, 48, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static final GUISliceFeature FRAME_LIGHT_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(68, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static final GUISliceFeature FRAME_LIGHT_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(85, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static final GUISliceFeature CIRCLE_WHITE = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(128, 96, 28, 28), new GUISpacing(14, 14, 13, 13));
	public static final GUISliceFeature CIRCLE_YELLOW = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(156, 96, 28, 28), new GUISpacing(14, 14, 13, 13));

	public static final GUISliceFeature FRAME_TAB = GUISliceFeature.inner(DEFAULT_TILESET, new GUIBox(448, 103, 17, 17),
			new GUISpacing(16, 8, 0, 8));

	public static final GUIPipeFeature PIPE = GUIPipeFeature.full(DEFAULT_TILESET, new GUIBox(0, 40, 120, 8));

	public static final GUIPipeFeature DRAG_LINES = GUIPipeFeature.dragLines(DEFAULT_TILESET,
			new GUIBox(192, 8, 24, 8));
	public static final GUIPipeFeature DRAG_LINES_WHITE = GUIPipeFeature.dragLines(DEFAULT_TILESET,
			new GUIBox(446, 78, 24, 8));

	public static final GUIStaticFeature ITEM_SLOT = new GUIStaticFeature(DEFAULT_TILESET, new GUIBox(0, 736, 80, 80));

	public static ImageDef DEF_CLOCK;

	public static Font createFont(String folder, String filename) {

		File folderFonts = new File(FactorioManager.getFolderDataRoot(), "fonts");
		folderFonts.mkdirs();

		File fileFont = new File(folderFonts, filename);

		try {
			if (FactorioManager.hasFactorioInstall()) {
				InputStream inputStream = FactorioManager.getBaseData().getModResource(folder + "/" + filename).get();
				Files.copy(inputStream, fileFont.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			return Font.createFont(Font.TRUETYPE_FONT, fileFont);
		} catch (FontFormatException | IOException e) {
			LOGGER.error("FAILED TO LOAD FONT: {}", fileFont.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	public static void initialize() {
		DEF_CLOCK = FactorioManager.getUtilitySprites().clock.defineSprites().get(0);
		AtlasManager.registerDef(DEF_CLOCK);
	}
}
