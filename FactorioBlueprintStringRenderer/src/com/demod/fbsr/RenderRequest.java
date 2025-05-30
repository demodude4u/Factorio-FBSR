package com.demod.fbsr;

import java.awt.Color;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.dcba.CommandReporting;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSColor;

public class RenderRequest {
	public static class Debug {
		public boolean pathItems = false;
		public boolean pathRails = false;
		public boolean entityPlacement = false;
	}

	public static class Show {
		public boolean altMode = true;
		public boolean pathOutputs = true;
		public boolean pathInputs = false;
		public boolean pathRails = true;
		public boolean gridNumbers = false;
		public boolean gridAboveBelts = false;
	}

	private BSBlueprint blueprint;
	private CommandReporting reporting;

	private OptionalInt maxWidth = OptionalInt.empty();
	private OptionalInt maxHeight = OptionalInt.empty();
	private OptionalInt minWidth = OptionalInt.empty();
	private OptionalInt minHeight = OptionalInt.empty();
	private OptionalDouble maxScale = OptionalDouble.empty();
	private boolean dontClipSprites = true;

	private Optional<Color> background = Optional.of(FBSR.GROUND_COLOR);
	private Optional<Color> gridLines = Optional.of(FBSR.GRID_COLOR);

	public final Debug debug = new Debug();
	public final Show show = new Show();

	public RenderRequest(BSBlueprint blueprint, CommandReporting reporting) {
		this.blueprint = blueprint;
		this.reporting = reporting;
	}

	public RenderRequest(BSBlueprint blueprint, CommandReporting reporting, JSONObject options) {
		this.blueprint = blueprint;
		this.reporting = reporting;

		parseOptions(options);
	}

	private void parseOptions(JSONObject json) {
		maxWidth = BSUtils.optInt(json, "max_width");
		maxHeight = BSUtils.optInt(json, "max_height");
		minWidth = BSUtils.optInt(json, "min_width");
		minHeight = BSUtils.optInt(json, "min_height");
		maxScale = BSUtils.optDouble(json, "max_scale");
		dontClipSprites = json.optBoolean("dont_clip_sprites", dontClipSprites);
		if (json.optBoolean("show_background", true)) {
			background = BSUtils.opt(json, "background", j -> new BSColor(j).createColor()).or(() -> background);
		} else {
			background = Optional.empty();
		}
		if (json.optBoolean("show_gridlines", true)) {
			gridLines = BSUtils.opt(json, "gridlines", j -> new BSColor(j).createColor()).or(() -> gridLines);
		} else {
			gridLines = Optional.empty();
		}
		debug.pathItems = json.optBoolean("debug_path_items", debug.pathItems);
		debug.pathRails = json.optBoolean("debug_path_rails", debug.pathRails);
		debug.entityPlacement = json.optBoolean("debug_entity_placement", debug.entityPlacement);
		show.altMode = json.optBoolean("show_alt_mode", show.altMode);
		show.pathOutputs = json.optBoolean("show_path_outputs", show.pathOutputs);
		show.pathInputs = json.optBoolean("show_path_inputs", show.pathInputs);
		show.pathRails = json.optBoolean("show_path_rails", show.pathRails);
		show.gridNumbers = json.optBoolean("show_grid_numbers", show.gridNumbers);
		show.gridAboveBelts = json.optBoolean("show_grid_above_belts", show.gridAboveBelts);
	}

	public Optional<Color> getBackground() {
		return background;
	}

	public BSBlueprint getBlueprint() {
		return blueprint;
	}

	public Optional<Color> getGridLines() {
		return gridLines;
	}

	public OptionalInt getMaxHeight() {
		return maxHeight;
	}

	public OptionalDouble getMaxScale() {
		return maxScale;
	}

	public OptionalInt getMaxWidth() {
		return maxWidth;
	}

	public OptionalInt getMinHeight() {
		return minHeight;
	}

	public OptionalInt getMinWidth() {
		return minWidth;
	}

	public CommandReporting getReporting() {
		return reporting;
	}

	public boolean dontClipSprites() {
		return dontClipSprites;
	}

	public void setBackground(Optional<Color> background) {
		this.background = background;
	}

	public void setBlueprint(BSBlueprint blueprint) {
		this.blueprint = blueprint;
	}

	public void setDontClipSprites(boolean dontClipSprites) {
		this.dontClipSprites = dontClipSprites;
	}

	public void setGridLines(Optional<Color> gridLines) {
		this.gridLines = gridLines;
	}

	public void setMaxHeight(OptionalInt maxHeight) {
		this.maxHeight = maxHeight;
	}

	public void setMaxScale(OptionalDouble maxScale) {
		this.maxScale = maxScale;
	}

	public void setMaxWidth(OptionalInt maxWidth) {
		this.maxWidth = maxWidth;
	}

	public void setMinHeight(OptionalInt minHeight) {
		this.minHeight = minHeight;
	}

	public void setMinWidth(OptionalInt minWidth) {
		this.minWidth = minWidth;
	}

	public void setReporting(CommandReporting reporting) {
		this.reporting = reporting;
	}

	

}
