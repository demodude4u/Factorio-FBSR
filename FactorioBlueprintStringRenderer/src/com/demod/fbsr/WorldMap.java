package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class WorldMap {

	public static enum BeltBend {
		FROM_LEFT, NONE, FROM_RIGHT;
	}

	public static class BeltCell {
		private final Direction facing;
		private final boolean bendable;
		private final boolean bendOthers;

		public BeltCell(Direction facing, boolean bendable, boolean bendOthers) {
			this.facing = facing;
			this.bendable = bendable;
			this.bendOthers = bendOthers;
		}

		public boolean canBendOthers() {
			return bendOthers;
		}

		public Direction getFacing() {
			return facing;
		}

		public boolean isBendable() {
			return bendable;
		}
	}

	public static class Debug {
		public boolean typeMapping = false;
		public boolean inputs = false;
		public boolean logistic = false;
		public boolean rail = false;
		public boolean placement = false;
		public boolean bounds = false;
	}

	public static class RailEdge {
		private final Point2D.Double startPos;
		private final Direction startDir;
		private final Point2D.Double endPos;
		private final Direction endDir;
		private final boolean curved;

		private boolean blocked = false;
		private boolean input = false;
		private boolean output = false;

		public RailEdge(Point2D.Double startPos, Direction startDir, Point2D.Double endPos, Direction endDir,
				boolean curved) {
			this.startPos = startPos;
			this.startDir = startDir;
			this.endPos = endPos;
			this.endDir = endDir;
			this.curved = curved;
		}

		public Direction getEndDir() {
			return endDir;
		}

		public Point2D.Double getEndPos() {
			return endPos;
		}

		public Direction getStartDir() {
			return startDir;
		}

		public Point2D.Double getStartPos() {
			return startPos;
		}

		public boolean isBlocked() {
			return blocked;
		}

		public boolean isCurved() {
			return curved;
		}

		public boolean isInput() {
			return input;
		}

		public boolean isOutput() {
			return output;
		}

		public void setBlocked(boolean blocked) {
			this.blocked = blocked;
		}

		public void setInput(boolean input) {
			this.input = input;
		}

		public void setOutput(boolean output) {
			this.output = output;
		}

	}

	public static class RailNode {
		private static class EdgeMap {
			private Optional<EdgePair> edgePair = Optional.empty();
			private Optional<Multimap<Direction, RailEdge>> edgeIntersection = Optional.empty();

			public void addEdge(Direction dir, RailEdge edge) {
				if (!edgeIntersection.isPresent() && !edgePair.isPresent()) {
					edgePair = Optional.of(new EdgePair(dir, edge));
				} else if (edgePair.isPresent() && (dir == edgePair.get().dir.back())
						&& !edgePair.get().rev.isPresent()) {
					edgePair.get().rev = Optional.of(edge);
				} else {
					if (!edgeIntersection.isPresent()) {
						EdgePair old = edgePair.get();
						edgePair = Optional.empty();
						LinkedHashMultimap<Direction, RailEdge> map = LinkedHashMultimap.create();
						edgeIntersection = Optional.of(map);
						map.put(old.dir, old.fwd);
						old.rev.ifPresent(e -> map.put(old.dir.back(), e));
					}
					edgeIntersection.get().put(dir, edge);
				}
			}

			public Collection<RailEdge> getEdges(Direction dir) {
				if (edgePair.isPresent()) {
					EdgePair p = edgePair.get();
					if (p.dir == dir) {
						return ImmutableList.of(p.fwd);
					} else if (p.rev.isPresent() && dir == p.dir.back()) {
						return ImmutableList.of(p.rev.get());
					} else {
						return ImmutableList.of();
					}
				} else if (edgeIntersection.isPresent()) {
					return edgeIntersection.get().get(dir);
				} else {
					return ImmutableList.of();
				}
			}
		}

		private static class EdgePair {
			private final Direction dir;
			private final RailEdge fwd;
			private Optional<RailEdge> rev = Optional.empty();

			public EdgePair(Direction dir, RailEdge fwd) {
				this.dir = dir;
				this.fwd = fwd;
			}
		}

		private final EdgeMap outgoingEdgeMap = new EdgeMap();
		private final EdgeMap incomingEdgeMap = new EdgeMap();
		private Optional<Set<Direction>> signals = Optional.empty();
		private Optional<Direction> station = Optional.empty();

		public void addIncomingEdge(RailEdge edge) {
			incomingEdgeMap.addEdge(edge.getEndDir(), edge);
		}

		public void addOutgoingEdge(RailEdge edge) {
			outgoingEdgeMap.addEdge(edge.getStartDir(), edge);
		}

		public Collection<RailEdge> getIncomingEdges(Direction dir) {
			return incomingEdgeMap.getEdges(dir);
		}

		public Collection<RailEdge> getOutgoingEdges(Direction dir) {
			return outgoingEdgeMap.getEdges(dir);
		}

		public Set<Direction> getSignals() {
			return signals.orElse(ImmutableSet.of());
		}

		public Optional<Direction> getStation() {
			return station;
		}

		public boolean hasSignals() {
			return signals.isPresent();
		}

		public boolean isSignal(Direction dir) {
			return signals.map(s -> s.contains(dir)).orElse(false);
		}

		public void setSignal(Direction dir) {
			if (!signals.isPresent()) {
				signals = Optional.of(new LinkedHashSet<>());
			}
			signals.get().add(dir);
		}

		public void setStation(Direction dir) {
			station = Optional.of(dir);
		}
	}

	// XXX Hash-based tables are not the most efficient here
	// Row: X
	// Column: Y
	private final Table<Integer, Integer, BeltCell> belts = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> pipes = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> heatPipes = HashBasedTable.create();
	private final Table<Integer, Integer, Object> walls = HashBasedTable.create();
	private final Table<Integer, Integer, Boolean> gates = HashBasedTable.create();
	private final Table<Integer, Integer, Entry<String, Direction>> undergroundBeltEndings = HashBasedTable.create();
	private final Table<Integer, Integer, List<BlueprintEntity>> beaconed = HashBasedTable.create();

	// Row: X*2
	// Column: Y*2
	private final Table<Integer, Integer, LogisticGridCell> logisticGrid = HashBasedTable.create();
	private final Table<Integer, Integer, RailNode> railNodes = HashBasedTable.create();

	// Key: "eid1|cid1|eid2|cid2|color"
	private final Map<String, Entry<Point2D.Double, Point2D.Double>> wires = new LinkedHashMap<>();

	private final List<Entry<RailEdge, RailEdge>> railEdges = new ArrayList<>();

	private Debug debug = new Debug();

	private int flag(Direction facing) {
		return 1 << facing.cardinal();
	}

	public Optional<List<BlueprintEntity>> getBeaconed(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return Optional.ofNullable(beaconed.get(kr, kc));
	}

	public Optional<BeltCell> getBelt(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return Optional.ofNullable(belts.get(kr, kc));
	}

	public Optional<BeltBend> getBeltBend(Point2D.Double pos) {
		return getBelt(pos).map(b -> {
			return getBeltBend(pos, b);
		});
	}

	public BeltBend getBeltBend(Point2D.Double pos, BeltCell belt) {
		if (!belt.bendable) {
			return BeltBend.NONE;
		}

		boolean left = isBeltFacingMeFrom(pos, belt.facing.left());
		boolean right = isBeltFacingMeFrom(pos, belt.facing.right());
		boolean back = isBeltFacingMeFrom(pos, belt.facing.back());

		if (back || (left && right)) {
			return BeltBend.NONE;
		} else if (left) {
			return BeltBend.FROM_LEFT;
		} else if (right) {
			return BeltBend.FROM_RIGHT;
		} else {
			return BeltBend.NONE;
		}
	}

	public Optional<Direction> getBeltFacing(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return Optional.ofNullable(belts.get(kr, kc)).map(BeltCell::getFacing);
	}

	public Debug getDebug() {
		return debug;
	}

	public Point2D.Double getLogisticCellPosition(Cell<Integer, Integer, LogisticGridCell> c) {
		return new Point2D.Double(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
	}

	public Table<Integer, Integer, LogisticGridCell> getLogisticGrid() {
		return logisticGrid;
	}

	public Optional<LogisticGridCell> getLogisticGridCell(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x * 2);
		int kc = (int) Math.floor(pos.y * 2);
		return Optional.ofNullable(logisticGrid.get(kr, kc));
	}

	public LogisticGridCell getOrCreateLogisticGridCell(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x * 2);
		int kc = (int) Math.floor(pos.y * 2);
		LogisticGridCell ret = logisticGrid.get(kr, kc);
		if (ret == null) {
			logisticGrid.put(kr, kc, ret = new LogisticGridCell());
		}
		return ret;
	}

	public RailNode getOrCreateRailNode(Point2D.Double pos) {
		int kr = (int) Math.round(pos.x * 2);
		int kc = (int) Math.round(pos.y * 2);
		RailNode ret = railNodes.get(kr, kc);
		if (ret == null) {
			railNodes.put(kr, kc, ret = new RailNode());
		}
		return ret;
	}

	public List<Entry<RailEdge, RailEdge>> getRailEdges() {
		return railEdges;
	}

	public Optional<RailNode> getRailNode(Point2D.Double pos) {
		int kr = (int) Math.round(pos.x * 2);
		int kc = (int) Math.round(pos.y * 2);
		return Optional.ofNullable(railNodes.get(kr, kc));
	}

	public Table<Integer, Integer, RailNode> getRailNodes() {
		return railNodes;
	}

	public Entry<Point2D.Double, Point2D.Double> getWire(String key) {
		return wires.get(key);
	}

	public Map<String, Entry<Point2D.Double, Point2D.Double>> getWires() {
		return wires;
	}

	public boolean hasWire(String key) {
		return wires.containsKey(key);
	}

	public boolean isBeltFacingMeFrom(Point2D.Double pos, Direction dir) {
		return getBelt(dir.offset(pos)).filter(b -> b.bendOthers).map(b -> b.facing)
				.map(d -> pos.distance(d.offset(dir.offset(pos))) < 0.1).orElse(false);
	}

	public boolean isHeatPipe(Point2D.Double pos, Direction facing) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return heatPipes.contains(kr, kc) && (heatPipes.get(kr, kc) & flag(facing)) > 0;
	}

	public boolean isHorizontalGate(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return gates.contains(kr, kc) && (gates.get(kr, kc) == false);
	}

	public boolean isMatchingUndergroundBeltEnding(String name, Point2D.Double pos, Direction dir) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return Optional.ofNullable(undergroundBeltEndings.get(kr, kc))
				.filter(p -> p.getKey().equals(name) && p.getValue().ordinal() == dir.ordinal()).isPresent();
	}

	public boolean isPipe(Point2D.Double pos, Direction facing) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return pipes.contains(kr, kc) && (pipes.get(kr, kc) & flag(facing)) > 0;
	}

	public boolean isVerticalGate(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return gates.contains(kr, kc) && (gates.get(kr, kc) == true);
	}

	public boolean isWall(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		return walls.contains(kr, kc);
	}

	public void setBeaconed(Point2D.Double pos, BlueprintEntity beacon) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		List<BlueprintEntity> list = beaconed.get(kr, kc);
		if (list == null) {
			beaconed.put(kr, kc, list = new LinkedList<>());
		}
		list.add(beacon);
	}

	public void setBelt(Point2D.Double pos, Direction facing, boolean bendable, boolean bendOthers) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		belts.put(kr, kc, new BeltCell(facing, bendable, bendOthers));
	}

	public void setDebug(Debug debug) {
		this.debug = debug;
	}

	public void setHeatPipe(Point2D.Double pos, Direction... facings) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		int flags = 0;
		if (facings.length == 0) {
			flags = 0b1111;
		} else {
			for (Direction facing : facings) {
				flags |= flag(facing);
			}
		}
		heatPipes.put(kr, kc, flags);
	}

	public void setHorizontalGate(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		gates.put(kr, kc, false);
	}

	public void setPipe(Point2D.Double pos, Direction... facings) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		int flags = 0;
		if (facings.length == 0) {
			flags = 0b1111;
		} else {
			for (Direction facing : facings) {
				flags |= flag(facing);
			}
		}
		pipes.put(kr, kc, flags);
	}

	public void setRailEdge(Double p1, Direction d1, Double p2, Direction d2, boolean curved) {
		RailNode node1 = getOrCreateRailNode(p1);
		RailNode node2 = getOrCreateRailNode(p2);
		RailEdge edge1 = new RailEdge(p1, d1, p2, d2, curved);
		node1.addOutgoingEdge(edge1);
		node2.addIncomingEdge(edge1);
		RailEdge edge2 = new RailEdge(p2, d2, p1, d1, curved);
		node2.addOutgoingEdge(edge2);
		node1.addIncomingEdge(edge2);
		railEdges.add(new SimpleEntry<>(edge1, edge2));
	}

	public void setUndergroundBeltEnding(String name, Point2D.Double pos, Direction dir) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		undergroundBeltEndings.put(kr, kc, new SimpleEntry<>(name, dir));
	}

	public void setVerticalGate(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		gates.put(kr, kc, true);
	}

	public void setWall(Point2D.Double pos) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		walls.put(kr, kc, pos);
	}

	public void setWire(String key, Entry<Point2D.Double, Point2D.Double> pair) {
		wires.put(key, pair);
	}
}
