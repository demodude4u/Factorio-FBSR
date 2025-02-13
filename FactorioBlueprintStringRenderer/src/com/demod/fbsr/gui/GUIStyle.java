package com.demod.fbsr.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.gui.feature.GUIPipeFeature;
import com.demod.fbsr.gui.feature.GUISliceFeature;
import com.demod.fbsr.gui.feature.GUIStaticFeature;

public final class GUIStyle {
	public static final Font FONT_BP_REGULAR = createFont("__core__/fonts/Lilittium-Regular.ttf");
	public static final Font FONT_BP_BOLD = createFont("__core__/fonts/Lilittium-Bold.ttf");
	public static final Color FONT_BP_COLOR = new Color(0xffe6c0);

	// TODO load these details from data.raw.gui-styles.default

	public static final String DEFAULT_TILESET = "__core__/graphics/gui-new.png";

	// frame
	public static GUISliceFeature FRAME_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(0, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	// inside_shallow_frame
	public static GUISliceFeature FRAME_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(17, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	// research_progress_inner_frame_inactive / technology_card_frame /
	// table_with_selection
	public static GUISliceFeature FRAME_DARK_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(34, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	// table_with_selection
	public static GUISliceFeature FRAME_DARK_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(51, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	public static GUISliceFeature FRAME_DARK_BUMP_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(282, 17, 17, 17), new GUISpacing(8, 8, 8, 8));

	// train_with_minimap_frame, shallow_frame, bonus_card_frame,
	// mods_explore_results_table, research_progress_inner_frame_active
	public static GUISliceFeature FRAME_LIGHT_INNER = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(68, 0, 17, 17), new GUISpacing(8, 8, 8, 8));
	// deep_scroll_pane, control_settings_section_frame, mod_thumbnail_image,
	// scroll_pane_in_shallow_frame,
	// list_box_in_shallow_frame_under_subheader_scroll_pane, tab_scroll_pane,
	// deep_frame_in_shallow_frame, text_holding_scroll_pane,
	// tab_deep_frame_in_entity_frame, shallow_frame_in_shallow_frame,
	// list_box_in_shallow_frame_scroll_pane
	public static GUISliceFeature FRAME_LIGHT_OUTER = GUISliceFeature.outer(DEFAULT_TILESET, //
			new GUIBox(85, 0, 17, 17), new GUISpacing(8, 8, 8, 8));

	public static GUISliceFeature CIRCLE_WHITE = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(128, 96, 28, 28), new GUISpacing(14, 14, 13, 13));
	public static GUISliceFeature CIRCLE_YELLOW = GUISliceFeature.inner(DEFAULT_TILESET, //
			new GUIBox(156, 96, 28, 28), new GUISpacing(14, 14, 13, 13));

	// Probably not used correctly...
	public static GUISliceFeature FRAME_TAB = GUISliceFeature.inner(DEFAULT_TILESET, new GUIBox(448, 103, 17, 17),
			new GUISpacing(16, 8, 0, 8));

	public static GUIPipeFeature PIPE = GUIPipeFeature.full(DEFAULT_TILESET, new GUIBox(0, 40, 120, 8));
	public static GUIPipeFeature DRAG_LINES = GUIPipeFeature.dragLines(DEFAULT_TILESET, new GUIBox(192, 8, 24, 8));

	public static GUIStaticFeature ITEM_SLOT = new GUIStaticFeature(DEFAULT_TILESET, new GUIBox(0, 736, 80, 80));

	public static Font createFont(String path) {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, FactorioManager.getBaseData().getModResource(path).get());
		} catch (FontFormatException | IOException e) {
			System.err.println("FAILED TO LOAD FONT: " + path);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
