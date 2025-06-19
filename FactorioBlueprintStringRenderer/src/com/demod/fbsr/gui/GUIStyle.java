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

import com.demod.factorio.DataTable;
import com.demod.factorio.ModLoader;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPUtilitySprites;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.feature.GUISliceFeature;
import com.demod.fbsr.gui.feature.GUIStaticFeature;

public final class GUIStyle {
	private static final Logger LOGGER = LoggerFactory.getLogger(GUIStyle.class);

	public Font FONT_BP_REGULAR;
	public Font FONT_BP_BOLD;
	public Color FONT_BP_COLOR;

	public GUISliceFeature FRAME_INNER;
	public GUISliceFeature FRAME_OUTER;
	public GUISliceFeature FRAME_DARK_INNER;
	public GUISliceFeature FRAME_DARK_OUTER;
	public GUISliceFeature FRAME_DARK_BUMP_OUTER;
	public GUISliceFeature FRAME_WHITE_INNER;
	public GUISliceFeature FRAME_WHITE_OUTER;
	public GUISliceFeature FRAME_WHITE_DARK_INNER;
	public GUISliceFeature FRAME_LIGHT_INNER;
	public GUISliceFeature FRAME_LIGHT_OUTER;
	public GUISliceFeature CIRCLE_WHITE;
	public GUISliceFeature CIRCLE_YELLOW;
	public GUISliceFeature FRAME_TAB;
	public GUIPipeFeature PIPE;
	public GUIPipeFeature DRAG_LINES;
	public GUIPipeFeature DRAG_LINES_WHITE;
	public GUIStaticFeature ITEM_SLOT;

	public ImageDef DEF_CLOCK;

	private static boolean copyFont(Profile profile, String filename) {
		if (!FactorioManager.hasFactorioInstall()) {
			LOGGER.error("Factorio installation not found, cannot copy font: {}", filename);
			return false;
		}

		File fileInstallFont = new File(FactorioManager.getFactorioInstall(), "data/core/fonts/" + filename);
		File fileProfileFont = new File(profile.getFolderProfile(), filename);
		try {
			Files.copy(fileInstallFont.toPath(), fileProfileFont.toPath(), StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Copied font: {}", fileProfileFont.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error("FAILED TO COPY FONT: {}", fileProfileFont.getAbsolutePath(), e);
			return false;
		}

		return true;
	}

	private static Font createFont(Profile profile, String filename) {
		File fileFont = new File(profile.getFolderProfile(), filename);
		try {
			return Font.createFont(Font.TRUETYPE_FONT, fileFont);
		} catch (FontFormatException | IOException e) {
			LOGGER.error("FAILED TO LOAD FONT: {}", fileFont.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	public static boolean copyFontsToProfile(Profile profile) {
		boolean ret = true;
		ret &= copyFont(profile, "Lilittium-Regular.ttf");
		ret &= copyFont(profile, "Lilittium-Bold.ttf");
		return ret;
	}

	public void initialize(Profile profile) {
		String filename = "__core__/graphics/gui-new.png";

		FONT_BP_REGULAR = createFont(profile, "Lilittium-Regular.ttf");
		FONT_BP_BOLD = createFont(profile, "Lilittium-Bold.ttf");
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

		DataTable table = profile.getFactorioData().getTable();
		FPUtilitySprites utilitySprites = new FPUtilitySprites(profile, table.getRaw("utility-sprites", "default").get());

		DEF_CLOCK = utilitySprites.clock.defineSprites().get(0);
		DEF_CLOCK.getProfile().getAtlasPackage().registerDef(DEF_CLOCK);
	}
}
