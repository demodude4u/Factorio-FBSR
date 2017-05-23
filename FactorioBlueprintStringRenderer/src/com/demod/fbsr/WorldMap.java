package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.util.Optional;

import com.demod.fbsr.BlueprintEntity.Direction;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class WorldMap {

	private final Table<Integer, Integer, Direction> belts = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> pipes = HashBasedTable.create();
	private final Table<Integer, Integer, Integer> heatPipes = HashBasedTable.create();
	private final Table<Integer, Integer, Object> walls = HashBasedTable.create();
	private final Table<Integer, Integer, Boolean> gates = HashBasedTable.create();

	private int flag(Direction facing) {
		return 1 << facing.cardinal();
	}

	public Optional<Direction> getBelt(Point2D.Double pos) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		return Optional.ofNullable(belts.get(kr, kc));
	}

	public boolean isHeatPipe(Point2D.Double pos, Direction facing) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		return heatPipes.contains(kr, kc) && (heatPipes.get(kr, kc) & flag(facing)) > 0;
	}

	public boolean isHorizontalGate(Point2D.Double pos) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		return gates.contains(kr, kc) && (gates.get(kr, kc) == false);
	}

	public boolean isPipe(Point2D.Double pos, Direction facing) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		return pipes.contains(kr, kc) && (pipes.get(kr, kc) & flag(facing)) > 0;
	}

	public boolean isVerticalGate(Point2D.Double pos) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		return gates.contains(kr, kc) && (gates.get(kr, kc) == true);
	}

	public boolean isWall(Point2D.Double pos) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		return walls.contains(kr, kc);
	}

	private int keyOf(double pos) {
		return (int) Math.round(pos - 0.5);
	}

	public void setBelt(Point2D.Double pos, Direction direction) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		belts.put(kr, kc, direction);
	}

	public void setHeatPipe(Point2D.Double pos, Direction... facings) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
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
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		gates.put(kr, kc, false);
	}

	public void setPipe(Point2D.Double pos, Direction... facings) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
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

	public void setVerticalGate(Point2D.Double pos) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		gates.put(kr, kc, true);
	}

	public void setWall(Point2D.Double pos) {
		int kr = keyOf(pos.x);
		int kc = keyOf(pos.y);
		walls.put(kr, kc, pos);
	}
}
