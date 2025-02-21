package com.demod.fbsr;

import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapRect3D;

public enum Direction {
	NORTH("N", 0, -1), //
	NORTHEAST("NE", 1, -1), //
	EAST("E", 1, 0), //
	SOUTHEAST("SE", 1, 1), //
	SOUTH("S", 0, 1), //
	SOUTHWEST("SW", -1, 1), //
	WEST("W", -1, 0), //
	NORTHWEST("NW", -1, -1);

	public static Direction fromCardinal(int cardinal) {
		return values()[cardinal * 2];
	}

	public static Direction fromSymbol(String symbol) {
		for (Direction direction : values()) {
			if (direction.symbol.equals(symbol)) {
				return direction;
			}
		}
		throw new IllegalArgumentException("Unknown symbol \"" + symbol + "\"");
	}

	private final String symbol;
	private final int dx;
	private final int dy;
	private final MapPosition offset;
	private final MapPosition offsetRight;
	private final double theta;
	private final double rotateSin;
	private final double rotateCos;

	private Direction(String symbol, int dx, int dy) {
		this.symbol = symbol;
		this.dx = dx;
		this.dy = dy;
		offset = MapPosition.byUnit(dx, dy);
		offsetRight = MapPosition.byUnit(dy, dx);
		theta = Math.atan2(dy, dx);
		rotateSin = Math.sin(theta);
		rotateCos = Math.cos(theta);
	}

	public int adjCode() {
		return 1 << ordinal();
	}

	public Direction back() {
		return rotate(4);
	}

	public Direction backLeft() {
		return rotate(-3);
	}

	public Direction backRight() {
		return rotate(3);
	}

	public int cardinal() {
		return ordinal() / 2;
	}

	public Direction frontLeft() {
		return rotate(-1);
	}

	public Direction frontRight() {
		return rotate(1);
	}

	public int getDx() {
		return dx;
	}

	public int getDy() {
		return dy;
	}

	public double getOrientation() {
		return ordinal() / 8.0;
	}

	public boolean isCardinal() {
		return (ordinal() % 2) == 0;
	}

	public boolean isHorizontal() {
		return this == EAST || this == WEST;
	}

	public boolean isVertical() {
		return this == NORTH || this == SOUTH;
	}

	public Direction left() {
		return rotate(-2);
	}

	public MapPosition offset() {
		return offset;
	}

	public MapPosition offset(double distance) {
		return offset.multiplyUnit(distance);
	}

	public MapPosition offset(MapPosition pos) {
		return offset.add(pos);
	}

	public MapPosition offset(MapPosition pos, double distance) {
		return offset.multiplyUnitAdd(distance, pos);
	}

//	public MapPosition offset(MapPosition pos, MapPosition offset) {
//		//TODO direct FP math instead of converting
//		return offset(right().offset(pos, offset.getY()), offset.getX());
//	}

//	public MapRect offset(MapRect rect, double distance) {
//		// TODO direct FP math instead of converting
//		return rect.add(offset(distance));
//	}

	public Direction right() {
		return rotate(2);
	}

	public Direction rotate(Direction dir) {
		return rotate(dir.ordinal());
	}

	public Direction rotate(int deltaIndex) {
		Direction[] values = values();
		return values[(((ordinal() + deltaIndex) % values.length) + values.length) % values.length];
	}

	public MapRect rotate(MapRect bounds) {
		if (this == NORTH) {
			return bounds;
		} else if (this == EAST) {
			return bounds.rotate90();
		} else if (this == SOUTH) {
			return bounds.rotate180();
		} else if (this == WEST) {
			return bounds.rotate270();
		} else {
			return bounds.transformMatrix(rotateCos, -rotateSin, rotateSin, rotateCos);
		}
	}

	public MapRect3D rotate(MapRect3D bounds) {
		if (this == NORTH) {
			return bounds;
		} else if (this == EAST) {
			return bounds.rotate90();
		} else if (this == SOUTH) {
			return bounds.rotate180();
		} else if (this == WEST) {
			return bounds.rotate270();
		} else {
			return bounds.transformMatrix(rotateCos, -rotateSin, rotateSin, rotateCos);
		}
	}

	public MapPosition rotate(MapPosition point) {
		if (this == NORTH) {
			return point;
		} else if (this == EAST) {
			return point.rotate90();
		} else if (this == SOUTH) {
			return point.rotate180();
		} else if (this == WEST) {
			return point.rotate270();
		} else {
			return point.transformMatrix(rotateCos, -rotateSin, rotateSin, rotateCos);
		}
	}
}