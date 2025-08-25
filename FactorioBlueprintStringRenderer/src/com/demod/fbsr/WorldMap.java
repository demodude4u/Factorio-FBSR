package com.demod.fbsr;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.demod.fbsr.entity.RailRendering.RailDef;
import com.demod.fbsr.entity.RailRendering.RailPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRail;
import com.demod.fbsr.map.MapRect;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class WorldMap {

	public static class BeaconSource {
		private final int row;
		private final int col;
		private final MapEntity beacon;
		private final double distributionEffectivity;

		public BeaconSource(int row, int col, MapEntity beacon, double distributionEffectivity) {
			this.row = row;
			this.col = col;
			this.beacon = beacon;
			this.distributionEffectivity = distributionEffectivity;
		}

		public MapEntity getBeacon() {
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

	// public static class RailEdge {
	// 	private final RailPoint start;
	// 	private final RailPoint end;

	// 	private boolean blocked = false;
	// 	private boolean input = false;
	// 	private boolean output = false;

	// 	private final List<List<RailSlot>> signalGroups = new ArrayList<>();
	// 	private final List<RailSlot> stationSlots = new ArrayList<>();

	// 	public RailEdge(RailPoint start, RailPoint end) {
	// 		this.start = start;
	// 		this.end = end;
	// 	}

	// 	public RailPoint getStart() {
	// 		return start;
	// 	}

	// 	public RailPoint getEnd() {
	// 		return end;
	// 	}

	// 	public boolean isBlocked() {
	// 		return blocked;
	// 	}

	// 	public boolean isInput() {
	// 		return input;
	// 	}

	// 	public boolean isOutput() {
	// 		return output;
	// 	}

	// 	public void setBlocked(boolean blocked) {
	// 		this.blocked = blocked;
	// 	}

	// 	public void setInput(boolean input) {
	// 		this.input = input;
	// 	}

	// 	public void setOutput(boolean output) {
	// 		this.output = output;
	// 	}

	// 	public void addSignalGroup(List<RailSlot> slots) {
	// 		signalGroups.add(slots);
	// 	}

	// 	public void addStation(RailSlot station) {
	// 		stationSlots.add(station);
	// 	}

	// 	public List<List<RailSlot>> getSignalGroups() {
	// 		return signalGroups;
	// 	}

	// 	public List<RailSlot> getStationSlots() {
	// 		return stationSlots;
	// 	}

	// }

	// public static class RailNode {
	// 	private static class EdgeMap {
	// 		private Optional<EdgePair> edgePair = Optional.empty();
	// 		private Optional<Multimap<RailDirection, RailEdge>> edgeIntersection = Optional.empty();

	// 		public void addEdge(RailDirection dir, RailEdge edge) {
	// 			if (!edgeIntersection.isPresent() && !edgePair.isPresent()) {
	// 				edgePair = Optional.of(new EdgePair(dir, edge));
	// 			} else if (edgePair.isPresent() && (dir == edgePair.get().dir.back())
	// 					&& !edgePair.get().rev.isPresent()) {
	// 				edgePair.get().rev = Optional.of(edge);
	// 			} else {
	// 				if (!edgeIntersection.isPresent()) {
	// 					EdgePair old = edgePair.get();
	// 					edgePair = Optional.empty();
	// 					LinkedHashMultimap<RailDirection, RailEdge> map = LinkedHashMultimap.create();
	// 					edgeIntersection = Optional.of(map);
	// 					map.put(old.dir, old.fwd);
	// 					old.rev.ifPresent(e -> map.put(old.dir.back(), e));
	// 				}
	// 				edgeIntersection.get().put(dir, edge);
	// 			}
	// 		}

	// 		public Collection<RailEdge> getEdges(RailDirection dir) {
	// 			if (edgePair.isPresent()) {
	// 				EdgePair p = edgePair.get();
	// 				if (p.dir == dir) {
	// 					return ImmutableList.of(p.fwd);
	// 				} else if (p.rev.isPresent() && dir == p.dir.back()) {
	// 					return ImmutableList.of(p.rev.get());
	// 				} else {
	// 					return ImmutableList.of();
	// 				}
	// 			} else if (edgeIntersection.isPresent()) {
	// 				return edgeIntersection.get().get(dir);
	// 			} else {
	// 				return ImmutableList.of();
	// 			}
	// 		}
	// 	}

	// 	private static class EdgePair {
	// 		private final RailDirection dir;
	// 		private final RailEdge fwd;
	// 		private Optional<RailEdge> rev = Optional.empty();

	// 		public EdgePair(RailDirection dir, RailEdge fwd) {
	// 			this.dir = dir;
	// 			this.fwd = fwd;
	// 		}
	// 	}

	// 	private final EdgeMap outgoingEdgeMap = new EdgeMap();
	// 	private final EdgeMap incomingEdgeMap = new EdgeMap();

	// 	public void addIncomingEdge(RailEdge edge) {
	// 		incomingEdgeMap.addEdge(edge.getEnd().dir, edge);
	// 	}

	// 	public void addOutgoingEdge(RailEdge edge) {
	// 		outgoingEdgeMap.addEdge(edge.getStart().dir, edge);
	// 	}

	// 	public Collection<RailEdge> getIncomingEdges(RailDirection dir) {
	// 		return incomingEdgeMap.getEdges(dir);
	// 	}

	// 	public Collection<RailEdge> getOutgoingEdges(RailDirection dir) {
	// 		return outgoingEdgeMap.getEdges(dir);
	// 	}
	// }

	// public static class RailSlot {
	// 	private EnumMap<RailDirection, MapEntity> entities = null;

	// 	public Optional<MapEntity> get(RailDirection dir) {
	// 		if (entities == null) {
	// 			return Optional.empty();
	// 		}
	// 		return Optional.ofNullable(entities.get(dir));
	// 	}

	// 	public void set(RailDirection dir, MapEntity entity) {
	// 		if (entities == null) {
	// 			entities = new EnumMap<>(RailDirection.class);
	// 		}
	// 		entities.put(dir, entity);
	// 	}
	// }

	// XXX Hash-based tables are not the most efficient here
	// Row: X
	// Column: Y
	private final Table<Integer, Integer, BeltCell> belts = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> pipes = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> pipePieceAdjCodes = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> heatPipes = HashBasedTable.create();
	private final Table<Integer, Integer, Object> walls = HashBasedTable.create();
	private final Table<Integer, Integer, Boolean> gates = HashBasedTable.create();
	private final Table<Integer, Integer, Entry<String, Direction>> undergroundBeltEndings = HashBasedTable.create();
	private final Table<Integer, Integer, List<BeaconSource>> beaconed = HashBasedTable.create();
	private final Table<Integer, Integer, MapEntity> cargoBayConnectables = HashBasedTable.create();
	private final Table<Integer, Integer, List<Boolean>> fusionConnections = HashBasedTable.create();
	private final Table<Integer, Integer, List<RailPoint>> railConnectionsGrounded = HashBasedTable.create();
	private final Table<Integer, Integer, List<RailPoint>> railConnectionsElevated = HashBasedTable.create();
	// private final Table<Integer, Integer, RailSlot> railSignalSlots = HashBasedTable.create();
	// private final Table<Integer, Integer, RailSlot> elevatedRailSignalSlots = HashBasedTable.create();
	// private final Table<Integer, Integer, RailNode> railNodes = HashBasedTable.create();
	// private final Table<Integer, Integer, RailNode> elevatedRailNodes = HashBasedTable.create();
	// private final Table<Integer, Integer, RailSlot> railStationSlots = HashBasedTable.create();

	// TODO a more generalized approach
	private final Table<Integer, Integer, MapEntity> nixieTubes = HashBasedTable.create();
	private final Table<Integer, Integer, MapEntity> elevatedPipes = HashBasedTable.create();

	// Row: X*2
	// Column: Y*2
	private final Table<Integer, Integer, LogisticGridCell> logisticGrid = HashBasedTable.create();

	// private final List<Entry<RailEdge, RailEdge>> railEdges = new ArrayList<>();

	private final List<MapRail> rails = new ArrayList<>();

	private final Set<String> unknownEntities = new HashSet<>();
	private final Set<String> unknownTiles = new HashSet<>();

	private boolean altMode = false;
	private boolean spaceFoundation = false;

	private ModdingResolver resolver;

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

	public List<Boolean> getFusionConnections(MapPosition pos) {
		return Optional.ofNullable(fusionConnections.get(pos.getXCell(), pos.getYCell())).orElse(ImmutableList.of());
	}

	public List<Boolean> getOrCreateFusionConnections(MapPosition pos) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		List<Boolean> ret = fusionConnections.get(kr, kc);
		if (ret == null) {
			fusionConnections.put(kr, kc, ret = new ArrayList<>());
		}
		return ret;
	}

	public MapPosition getLogisticCellPosition(Cell<Integer, Integer, LogisticGridCell> c) {
		return MapPosition.byUnit(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
	}

	public Table<Integer, Integer, LogisticGridCell> getLogisticGrid() {
		return logisticGrid;
	}

	public Optional<LogisticGridCell> getLogisticGridCell(MapPosition pos) {
		int kr = pos.getXHalfCell();
		int kc = pos.getYHalfCell();
		return Optional.ofNullable(logisticGrid.get(kr, kc));
	}

	public List<LogisticGridCell> getLogisticGridCells(MapRect rect) {
		List<LogisticGridCell> cells = new ArrayList<>();

		// TODO use fixed point math

		double startX = Math.round(rect.getX() * 2.0) / 2.0 + 0.25;
		double startY = Math.round(rect.getY() * 2.0) / 2.0 + 0.25;
		double endX = rect.getX() + rect.getWidth();
		double endY = rect.getY() + rect.getHeight();

		for (double y = startY; y < endY; y += 0.5) {
			for (double x = startX; x < endX; x += 0.5) {
				getLogisticGridCell(MapPosition.byUnit(x, y)).ifPresent(cells::add);
			}
		}

		return cells;
	}

	public Optional<MapEntity> getNixieTube(MapPosition pos) {
		return Optional.ofNullable(nixieTubes.get(pos.getXCell(), pos.getYCell()));
	}

	public LogisticGridCell getOrCreateLogisticGridCell(MapPosition pos) {
		int kr = pos.getXHalfCell();
		int kc = pos.getYHalfCell();
		LogisticGridCell ret = logisticGrid.get(kr, kc);
		if (ret == null) {
			logisticGrid.put(kr, kc, ret = new LogisticGridCell());
		}
		return ret;
	}

	public OptionalInt getPipePieceAdjCode(MapPosition pos) {
		int kr = pos.getXCell();
		int kc = pos.getYCell();
		if (pipePieceAdjCodes.contains(kr, kc)) {
			return OptionalInt.of(pipePieceAdjCodes.get(kr, kc));
		}
		return OptionalInt.empty();
	}

	// public RailNode getOrCreateRailNode(MapPosition pos, boolean elevated) {
	// 	int kr = pos.getXCell();
	// 	int kc = pos.getYCell();
	// 	Table<Integer, Integer, RailNode> table = elevated ? elevatedRailNodes : railNodes;
	// 	RailNode ret = table.get(kr, kc);
	// 	if (ret == null) {
	// 		table.put(kr, kc, ret = new RailNode());
	// 	}
	// 	return ret;
	// }

	// public RailSlot getOrCreateSignalSlot(MapPosition pos, boolean elevated) {
	// 	int kr = pos.getXCell();
	// 	int kc = pos.getYCell();
	// 	Table<Integer, Integer, RailSlot> table = elevated ? elevatedRailSignalSlots : railSignalSlots;
	// 	RailSlot ret = table.get(kr, kc);
	// 	if (ret == null) {
	// 		table.put(kr, kc, ret = new RailSlot());
	// 	}
	// 	return ret;
	// }

	// public RailSlot getOrCreateStationSlot(MapPosition pos) {
	// 	int kr = pos.getXCell();
	// 	int kc = pos.getYCell();
	// 	RailSlot ret = railStationSlots.get(kr, kc);
	// 	if (ret == null) {
	// 		railStationSlots.put(kr, kc, ret = new RailSlot());
	// 	}
	// 	return ret;
	// }

	// public List<Entry<RailEdge, RailEdge>> getRailEdges() {
	// 	return railEdges;
	// }

	// public Optional<RailNode> getRailNode(MapPosition pos, boolean elevated) {
	// 	int kr = pos.getXCell();
	// 	int kc = pos.getYCell();
	// 	return Optional.ofNullable(elevated ? elevatedRailNodes.get(kr, kc) : railNodes.get(kr, kc));
	// }

	// public Optional<RailSlot> getRailSignalSlot(MapPosition pos, boolean elevated) {
	// 	int kr = pos.getXCell();
	// 	int kc = pos.getYCell();
	// 	return Optional.ofNullable(elevated ? elevatedRailSignalSlots.get(kr, kc) : railSignalSlots.get(kr, kc));
	// }

	// public Optional<RailSlot> getRailStationSlot(MapPosition pos) {
	// 	int kr = pos.getXCell();
	// 	int kc = pos.getYCell();
	// 	return Optional.ofNullable(railStationSlots.get(kr, kc));
	// }

	// public Table<Integer, Integer, RailNode> getRailNodes(boolean elevated) {
	// 	return elevated ? railNodes : elevatedRailNodes;
	// }

	public boolean isAltMode() {
		return altMode;
	}

	public void setResolver(ModdingResolver resolver) {
		this.resolver = resolver;
	}

	public ModdingResolver getResolver() {
		return resolver;
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

	public boolean isElevatedPipe(MapPosition pos) {
		return elevatedPipes.contains(pos.getXCell(), pos.getYCell());
	}

	public boolean isSpaceFoundation() {
		return spaceFoundation;
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

	public void setBeaconed(MapPosition pos, MapEntity beacon, double distributionEffectivity) {
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

	public void setCargoBayConnectable(MapPosition pos, MapEntity entity) {
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

	public void setNixieTube(MapPosition pos, MapEntity entity) {
		nixieTubes.put(pos.getXCell(), pos.getYCell(), entity);
	}

	public void setElevatedPipe(MapPosition pos, MapEntity entity) {
		elevatedPipes.put(pos.getXCell(), pos.getYCell(), entity);
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
		Integer currentFlags = pipes.get(pos.getXCell(), pos.getYCell());
		if (currentFlags != null) {
			flags |= currentFlags;
		}
		pipes.put(pos.getXCell(), pos.getYCell(), flags);
	}

	public void setPipePieceAdjCode(MapPosition pos, int adjCode) {
		pipePieceAdjCodes.put(pos.getXCell(), pos.getYCell(), adjCode);
	}

	public void setSpaceFoundation(boolean foundation) {
		this.spaceFoundation = foundation;
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

	public void setRail(MapRail rail) {
		MapPosition pos = rail.getPos();
		RailDef def = rail.getDef();

		MapPosition p1 = def.A.pos.add(pos);
		MapPosition p2 = def.B.pos.add(pos);

		setRailConnection(rail, def.A);
		setRailConnection(rail, def.B);

		rails.add(rail);
	}

	public void setRailConnection(MapRail rail, RailPoint point) {
		MapPosition pos = rail.getPos().add(point.pos);
		int kr = pos.getXCell();
		int kc = pos.getYCell();

		Table<Integer, Integer, List<RailPoint>> railConnections = point.elevated ? railConnectionsElevated : railConnectionsGrounded;
		List<RailPoint> list = railConnections.get(kr, kc);
		if (list == null) {
			railConnections.put(kr, kc, list = new ArrayList<>());
		}
		list.add(point);
	}

	public boolean isRailConnected(MapRail rail, RailPoint point) {
		MapPosition pos = rail.getPos().add(point.pos);
		int kr = pos.getXCell();
		int kc = pos.getYCell();

		Table<Integer, Integer, List<RailPoint>> railConnections = point.elevated ? railConnectionsElevated : railConnectionsGrounded;
		List<RailPoint> list = railConnections.get(kr, kc);
		if (list == null) {
			return false;
		}

		boolean checkElevated = point.elevated;
		Dir16 checkDir = point.dir.back();

		for (RailPoint r : list) {
			if (checkElevated == r.elevated && checkDir == r.dir) {
				return true;
			}
		}
		return false;
	}

	public List<MapRail> getRails() {
		return rails;
	}
}
