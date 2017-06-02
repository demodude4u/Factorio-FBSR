package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class LogisticGridCell {
	private Optional<Direction> move = Optional.empty();
	private Optional<Direction> acceptFilter = Optional.empty();
	private Optional<Point2D.Double> warp = Optional.empty();
	private Optional<Set<String>> inputs = Optional.empty();
	private Optional<Set<String>> outputs = Optional.empty();

	private Optional<SortedSet<String>> transits = Optional.empty();
	private boolean blockTransit = false;
	private Optional<List<Direction>> movedFrom = Optional.empty();
	private Optional<List<Point2D.Double>> warpedFrom = Optional.empty();

	public boolean acceptMoveFrom(Direction move) {
		if (acceptFilter.isPresent()) {
			return acceptFilter.get().ordinal() == move.ordinal();
		}
		return true;
	}

	public void addInput(String itemName) {
		if (!inputs.isPresent()) {
			inputs = Optional.of(new LinkedHashSet<>());
		}
		inputs.get().add(itemName);
	}

	public void addMovedFrom(Direction dir) {
		if (!movedFrom.isPresent()) {
			movedFrom = Optional.of(new ArrayList<>());
		}
		movedFrom.get().add(dir);
	}

	public void addOutput(String itemName) {
		if (!outputs.isPresent()) {
			outputs = Optional.of(new LinkedHashSet<>());
		}
		outputs.get().add(itemName);
	}

	public boolean addTransit(String itemName) {
		if (!transits.isPresent()) {
			transits = Optional.of(new TreeSet<>());
		}
		return transits.get().add(itemName);
	}

	public void addWarpedFrom(Point2D.Double pos) {
		if (!warpedFrom.isPresent()) {
			warpedFrom = Optional.of(new ArrayList<>());
		}
		warpedFrom.get().add(pos);
	}

	public Optional<Direction> getAcceptFilter() {
		return acceptFilter;
	}

	public Optional<Set<String>> getInputs() {
		return inputs;
	}

	public Optional<Direction> getMove() {
		return move;
	}

	public Optional<List<Direction>> getMovedFrom() {
		return movedFrom;
	}

	public Optional<Set<String>> getOutputs() {
		return outputs;
	}

	public Optional<SortedSet<String>> getTransits() {
		return transits;
	}

	public Optional<Point2D.Double> getWarp() {
		return warp;
	}

	public Optional<List<Point2D.Double>> getWarpedFrom() {
		return warpedFrom;
	}

	public boolean isAccepting() {
		return move.isPresent() || warp.isPresent() || inputs.isPresent();
	}

	public boolean isBlockTransit() {
		return blockTransit;
	}

	public boolean isTransitEnd() {
		return getInputs().isPresent() && (getWarpedFrom().map(l -> !l.isEmpty()).isPresent()
				|| getMovedFrom().map(l -> !l.isEmpty()).isPresent());
	}

	public boolean isTransitStart() {
		return getOutputs().isPresent() && (getWarp().isPresent() || getMove().isPresent());
	}

	public void setAcceptFilter(Optional<Direction> acceptFilter) {
		this.acceptFilter = acceptFilter;
	}

	public void setBlockTransit(boolean blockTransit) {
		this.blockTransit = blockTransit;
	}

	public void setInputs(Optional<Set<String>> inputs) {
		this.inputs = inputs;
	}

	public void setMove(Optional<Direction> move) {
		this.move = move;
	}

	public void setOutputs(Optional<Set<String>> outputs) {
		this.outputs = outputs;
	}

	public void setWarp(Optional<Point2D.Double> warp) {
		this.warp = warp;
	}

}
