package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import javafx.util.Pair;

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
	}

	// XXX Hash-based tables are not the most efficient here
	// Row: X
	// Column: Y
	private final Table<Integer, Integer, BeltCell> belts = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> pipes = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> heatPipes = HashBasedTable.create();
	private final Table<Integer, Integer, Object> walls = HashBasedTable.create();
	private final Table<Integer, Integer, Boolean> gates = HashBasedTable.create();
	private final Table<Integer, Integer, Pair<String, Direction>> undergroundBeltEndings = HashBasedTable.create();

	// Row: X*2
	// Column: Y*2
	private final Table<Integer, Integer, LogisticGridCell> logisticGrid = HashBasedTable.create();

	// Key: "eid1|cid1|eid2|cid2|color"
	private final Map<String, Pair<Point2D.Double, Point2D.Double>> wires = new LinkedHashMap<>();

	private Debug debug = new Debug();

	private int flag(Direction facing) {
		return 1 << facing.cardinal();
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

	public Pair<Point2D.Double, Point2D.Double> getWire(String key) {
		return wires.get(key);
	}

	public Map<String, Pair<Point2D.Double, Point2D.Double>> getWires() {
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

	public void setUndergroundBeltEnding(String name, Point2D.Double pos, Direction dir) {
		int kr = (int) Math.floor(pos.x);
		int kc = (int) Math.floor(pos.y);
		undergroundBeltEndings.put(kr, kc, new Pair<>(name, dir));
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

	public void setWire(String key, Pair<Point2D.Double, Point2D.Double> pair) {
		wires.put(key, pair);
	}
}
