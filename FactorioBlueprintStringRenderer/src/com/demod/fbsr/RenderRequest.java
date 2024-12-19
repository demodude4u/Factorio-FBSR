package com.demod.fbsr;

import java.awt.Color;
import java.util.Optional;
import java.util.OptionalInt;

import com.demod.dcba.CommandReporting;
import com.demod.fbsr.bs.BSBlueprint;

public class RenderRequest {
	public static class Debug {
		public boolean typeMapping = false;
		public boolean inputs = false;
		public boolean logistic = false;
		public boolean rail = false;
		public boolean placement = false;
		public boolean bounds = false;
	}

	private BSBlueprint blueprint;
	private CommandReporting reporting;

	private OptionalInt maxWidth = OptionalInt.empty();
	private OptionalInt maxHeight = OptionalInt.empty();
	private OptionalInt minWidth = OptionalInt.empty();
	private OptionalInt minHeight = OptionalInt.empty();

	private Optional<Color> background = Optional.of(FBSR.GROUND_COLOR);
	private Optional<Color> gridLines = Optional.of(FBSR.GRID_COLOR);

	public final Debug debug = new Debug();

	public RenderRequest(BSBlueprint blueprint, CommandReporting reporting) {
		this.blueprint = blueprint;
		this.reporting = reporting;
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

	public void setBackground(Optional<Color> background) {
		this.background = background;
	}

	public void setBlueprint(BSBlueprint blueprint) {
		this.blueprint = blueprint;
	}

	public void setGridLines(Optional<Color> gridLines) {
		this.gridLines = gridLines;
	}

	public void setMaxHeight(OptionalInt maxHeight) {
		this.maxHeight = maxHeight;
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