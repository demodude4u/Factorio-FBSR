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
	private Optional<List<Point2D.Double>> warps = Optional.empty();
	private Optional<Set<String>> inputs = Optional.empty();
	private Optional<Set<String>> outputs = Optional.empty();
	private Optional<Set<String>> bannedOutputs = Optional.empty();

	private Optional<SortedSet<String>> transits = Optional.empty();
	private boolean blockTransit = false;
	// Not implemented in FBSR::populateReverseLogistics
	private boolean blockWarpToIfMove = false; // dont warp to here if origin has a "move".
	// Not implemented in FBSR::populateReverseLogistics
	private boolean blockWarpFromIfMove = false; // dont warp from here if destination has a "move".
	private Optional<List<Direction>> movedFrom = Optional.empty();
	private Optional<List<Point2D.Double>> warpedFrom = Optional.empty();

	public boolean acceptMoveFrom(Direction move) {
		if (acceptFilter.isPresent()) {
			return acceptFilter.get().ordinal() == move.ordinal();
		}
		return true;
	}

	public void addBannedOutput(String itemName) {
		if (!bannedOutputs.isPresent()) {
			bannedOutputs = Optional.of(new LinkedHashSet<>());
		}
		bannedOutputs.get().add(itemName);
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

	public void addWarp(Point2D.Double warp) {
		if (!warps.isPresent()) {
			warps = Optional.of(new ArrayList<>());
		}
		this.warps.get().add(warp);
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

	public Optional<Set<String>> getBannedOutputs() {
		return bannedOutputs;
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

	public Optional<List<Point2D.Double>> getWarpedFrom() {
		return warpedFrom;
	}

	public Optional<List<Point2D.Double>> getWarps() {
		return warps;
	}

	public boolean isAccepting() {
		return move.isPresent() || warps.isPresent() || inputs.isPresent();
	}

	public boolean isBannedOutput(String item) {
		if (bannedOutputs.isPresent()) {
			return bannedOutputs.get().contains(item);
		}
		return false;
	}

	public boolean isBlockTransit() {
		return blockTransit;
	}

	public boolean isBlockWarpFromIfMove() {
		return blockWarpFromIfMove;
	}

	public boolean isBlockWarpToIfMove() {
		return blockWarpToIfMove;
	}

	public boolean isTransitEnd() {
		return inputs.isPresent()
				&& (warpedFrom.map(l -> !l.isEmpty()).isPresent() || movedFrom.map(l -> !l.isEmpty()).isPresent());
	}

	public boolean isTransitStart() {
		return outputs.isPresent() && (warps.isPresent() || move.isPresent());
	}

	public void setAcceptFilter(Optional<Direction> acceptFilter) {
		this.acceptFilter = acceptFilter;
	}

	public void setBannedOutputs(Optional<Set<String>> bannedOutputs) {
		this.bannedOutputs = bannedOutputs;
	}

	public void setBlockTransit(boolean blockTransit) {
		this.blockTransit = blockTransit;
	}

	public void setBlockWarpFromIfMove(boolean blockWarpFromIfMove) {
		this.blockWarpFromIfMove = blockWarpFromIfMove;
	}

	public void setBlockWarpToIfMove(boolean blockWarpToIfMove) {
		this.blockWarpToIfMove = blockWarpToIfMove;
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

}
