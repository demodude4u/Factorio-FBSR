package com.demod.fbsr;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.map.MapPosition;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class WorldMap {

	public static class BeaconSource {
		private final int row;
		private final int col;
		private final BSEntity beacon;
		private final double distributionEffectivity;

		public BeaconSource(int row, int col, BSEntity beacon, double distributionEffectivity) {
			this.row = row;
			this.col = col;
			this.beacon = beacon;
			this.distributionEffectivity = distributionEffectivity;
		}

		public BSEntity getBeacon() {
			return beacon;
		}

		public int getCol() {
			return col;
		}

		public double getDistributionEffectivity() {
			return distributionEffectivity;
		}

		public int getRow() {
			return row;
		}
	}

	public static enum BeltBend {
		FROM_LEFT(d -> d.left()), //
		NONE(d -> d.back()), //
		FROM_RIGHT(d -> d.right()),//
		;

		private final Function<Direction, Direction> rotation;

		private BeltBend(Function<Direction, Direction> rotation) {
			this.rotation = rotation;
		}

		public Direction reverse(Direction dir) {
			return rotation.apply(dir);
		}
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

	public static class RailEdge {
		private final MapPosition startPos;
		private final Direction startDir;
		private final MapPosition endPos;
		private final Direction endDir;
		private final boolean curved;

		private boolean blocked = false;
		private boolean input = false;
		private boolean output = false;

		public RailEdge(MapPosition startPos, Direction startDir, MapPosition endPos, Direction endDir,
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

		public MapPosition getEndPos() {
			return endPos;
		}

		public Direction getStartDir() {
			return startDir;
		}

		public MapPosition getStartPos() {
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
	private final Table<Integer, Integer, List<BeaconSource>> beaconed = HashBasedTable.create();
	private final Table<Integer, Integer, BSEntity> cargoBayConnectables = HashBasedTable.create();

	// Row: X*2
	// Column: Y*2
	private final Table<Integer, Integer, LogisticGridCell> logisticGrid = HashBasedTable.create();
	private final Table<Integer, Integer, RailNode> railNodes = HashBasedTable.create();

	private final List<Entry<RailEdge, RailEdge>> railEdges = new ArrayList<>();

	private final Set<String> unknownEntities = new HashSet<>();
	private final Set<String> unknownTiles = new HashSet<>();

	private boolean altMode = false;
	private boolean foundation = false;

	public boolean addUnknownEntity(String name) {
		return unknownEntities.add(name);
	}

	public boolean addUnknownTile(String name) {
		return unknownTiles.add(name);
	}

	private int flag(Direction facing) {
		return 1 << facing.cardinal();
	}

	public Optional<List<BeaconSource>> getBeaconed(MapPosition pos) {
		return Optional.ofNullable(beaconed.get(pos.getXCell(), pos.getYCell()));
	}

	public Optional<BeltCell> getBelt(MapPosition pos) {
		return Optional.ofNullable(belts.get(pos.getXCell(), pos.getYCell()));
	}

	public Optional<BeltBend> getBeltBend(MapPosition pos) {
		return getBelt(pos).map(b -> {
			return getBeltBend(pos, b);
		});
	}

	public BeltBend getBeltBend(MapPosition pos, BeltCell belt) {
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

	public Optional<Direction> getBeltFacing(MapPosition pos) {
		return Optional.ofNullable(belts.get(pos.getXCell(), pos.getYCell())).map(BeltCell::getFacing);
	}

	public MapPosition getLogisticCellPosition(Cell<Integer, Integer, LogisticGridCell> c) {
		return MapPosition.byUnit(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
	}

	public Table<Integer, Integer, LogisticGridCell> getLogisticGrid() {
		return logisticGrid;
	}

	public Optional<LogisticGridCell> getLogisticGridCell(MapPosition pos) {
		return Optional.ofNullable(logisticGrid.get(pos.getXCell() * 2, pos.getYCell() * 2));
	}

	public LogisticGridCell getOrCreateLogisticGridCell(MapPosition pos) {
		int kr = pos.getXCell() * 2;
		int kc = pos.getYCell() * 2;
		LogisticGridCell ret = logisticGrid.get(kr, kc);
		if (ret == null) {
			logisticGrid.put(kr, kc, ret = new LogisticGridCell());
		}
		return ret;
	}

	public RailNode getOrCreateRailNode(MapPosition pos) {
		int kr = pos.getXCell() * 2;
		int kc = pos.getYCell() * 2;
		RailNode ret = railNodes.get(kr, kc);
		if (ret == null) {
			railNodes.put(kr, kc, ret = new RailNode());
		}
		return ret;
	}

	public List<Entry<RailEdge, RailEdge>> getRailEdges() {
		return railEdges;
	}

	public Optional<RailNode> getRailNode(MapPosition pos) {
		return Optional.ofNullable(railNodes.get(pos.getXCell() * 2, pos.getYCell() * 2));
	}

	public Table<Integer, Integer, RailNode> getRailNodes() {
		return railNodes;
	}

	public boolean isAltMode() {
		return altMode;
	}

	public boolean isBeltFacingMeFrom(MapPosition pos, Direction dir) {
		Optional<BeltCell> optAdjBelt = getBelt(dir.offset(pos));
		if (optAdjBelt.isEmpty()) {
			return false;
		}
		BeltCell adjBelt = optAdjBelt.get();
		return adjBelt.bendOthers && (dir.back() == adjBelt.facing);
	}

	public boolean isCargoBayConnectable(MapPosition pos) {
		return cargoBayConnectables.contains(pos.getXCell(), pos.getYCell());
	}

	public boolean isHeatPipe(MapPosition pos, Direction facing) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		return heatPipes.contains(kr, kc) && (heatPipes.get(kr, kc) & flag(facing)) > 0;
	}

	public boolean isHorizontalGate(MapPosition pos) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		return gates.contains(kr, kc) && (gates.get(kr, kc) == false);
	}

	public boolean isMatchingUndergroundBeltEnding(String name, MapPosition pos, Direction dir) {
		return Optional.ofNullable(undergroundBeltEndings.get(pos.getXCell(), pos.getYCell()))
				.filter(p -> p.getKey().equals(name) && p.getValue().ordinal() == dir.ordinal()).isPresent();
	}

	public boolean isPipe(MapPosition pos, Direction facing) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		return pipes.contains(kr, kc) && (pipes.get(kr, kc) & flag(facing)) > 0;
	}

	public boolean isFoundation() {
		return foundation;
	}

	public boolean isVerticalGate(MapPosition pos) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		return gates.contains(kr, kc) && (gates.get(kr, kc) == true);
	}

	public boolean isWall(MapPosition pos) {
		return walls.contains(pos.getXCell(), pos.getYCell());
	}

	public void setAltMode(boolean altMode) {
		this.altMode = altMode;
	}

	public void setBeaconed(MapPosition pos, BSEntity beacon, double distributionEffectivity) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		List<BeaconSource> list = beaconed.get(kr, kc);
		if (list == null) {
			beaconed.put(kr, kc, list = new LinkedList<>());
		}
		list.add(new BeaconSource(kr, kc, beacon, distributionEffectivity));
	}

	public void setBelt(MapPosition pos, Direction facing, boolean bendable, boolean bendOthers) {
		belts.put(pos.getXCell(), pos.getYCell(), new BeltCell(facing, bendable, bendOthers));
	}

	public void setCargoBayConnectable(MapPosition pos, BSEntity entity) {
		cargoBayConnectables.put(pos.getXCell(), pos.getYCell(), entity);
	}

	public void setHeatPipe(MapPosition pos, Direction... facings) {
		int flags = 0;
		if (facings.length == 0) {
			flags = 0b1111;
		} else {
			for (Direction facing : facings) {
				flags |= flag(facing);
			}
		}
		heatPipes.put(pos.getXCell(), pos.getYCell(), flags);
	}

	public void setHorizontalGate(MapPosition pos) {
		gates.put(pos.getXCell(), pos.getYCell(), false);
	}

	public void setPipe(MapPosition pos, Direction... facings) {
		int flags = 0;
		if (facings.length == 0) {
			flags = 0b1111;
		} else {
			for (Direction facing : facings) {
				flags |= flag(facing);
			}
		}
		pipes.put(pos.getXCell(), pos.getYCell(), flags);
	}

	public void setRailEdge(MapPosition p1, Direction d1, MapPosition p2, Direction d2, boolean curved) {
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

	public void setFoundation(boolean foundation) {
		this.foundation = foundation;
	}

	public void setUndergroundBeltEnding(String name, MapPosition pos, Direction dir) {
		undergroundBeltEndings.put(pos.getXCell(), pos.getYCell(), new SimpleEntry<>(name, dir));
	}

	public void setVerticalGate(MapPosition pos) {
		gates.put(pos.getXCell(), pos.getYCell(), true);
	}

	public void setWall(MapPosition pos) {
		walls.put(pos.getXCell(), pos.getYCell(), pos);
	}
}
