package com.demod.fbsr.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import com.demod.factorio.FactorioData;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.feature.GUISliceFeature;
import com.demod.fbsr.gui.feature.GUIStaticFeature;

//I could not find this information in the factorio data, might be hard-coded
public final class GUIStyle {
	public static final Font FONT_BP_REGULAR = createFont("__core__/fonts/Lilittium-Regular.ttf");
	public static final Font FONT_BP_BOLD = createFont("__core__/fonts/Lilittium-Bold.ttf");
	public static final Color FONT_BP_COLOR = new Color(0xffe6c0);

	public static final String DEFAULT_TILESET = "__core__/graphics/gui-new.png";

	// TODO define all of the inner and outer panels I care to use
	public static GUISliceFeature FRAME_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(0, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static GUISliceFeature FRAME_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(17, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static GUISliceFeature FRAME_DARK_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(34, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static GUISliceFeature FRAME_DARK_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(51, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static GUISliceFeature FRAME_LIGHT_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(68, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static GUISliceFeature FRAME_LIGHT_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(85, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static GUIPipeFeature PIPE = GUIPipeFeature.full(DEFAULT_TILESET, new GUIBox(0, 40, 120, 8));

	public static GUIStaticFeature ITEM_SLOT = new GUIStaticFeature(DEFAULT_TILESET, new GUIBox(0, 736, 80, 80));

	public static Font createFont(String path) {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, FactorioData.getModResource(path).get());
		} catch (FontFormatException | IOException e) {
			System.err.println("FAILED TO LOAD FONT: " + path);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
