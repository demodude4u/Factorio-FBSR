package demod.fbsr;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;

import org.luaj.vm2.LuaTable;

public class BlueprintEntity {
	public static enum Direction {
		NORTH(0, -1), //
		NORTHEAST(1, -1), //
		EAST(1, 0), //
		SOUTHEAST(1, 1), //
		SOUTH(0, 1), //
		SOUTHWEST(-1, 1), //
		WEST(-1, 0), //
		NORTHWEST(-1, -1);

		public static Direction fromCardinal(int cardinal) {
			return values()[cardinal * 2];
		}

		private final int dx;
		private final int dy;

		private Direction(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}

		public Direction back() {
			return rotate(4);
		}

		public int cardinal() {
			return ordinal() / 2;
		}

		public Direction left() {
			return rotate(-2);
		}

		public Point2D.Double offset(Point2D.Double pos) {
			return new Point2D.Double(pos.x + dx, pos.y + dy);
		}

		public Double offset(Point2D.Double pos, double distance) {
			return new Point2D.Double(pos.x + distance * dx, pos.y + distance * dy);
		}

		public Rectangle2D.Double offset(Rectangle2D.Double rect, double distance) {
			return new Rectangle2D.Double(rect.x + distance * dx, rect.y + distance * dy, rect.width, rect.height);
		}

		public Direction right() {
			return rotate(2);
		}

		public Direction rotate(int deltaIndex) {
			Direction[] values = values();
			return values[(((ordinal() + deltaIndex) % values.length) + values.length) % values.length];
		}
	}

	private final LuaTable lua;
	private final String name;
	private final Double position;
	private final Direction direction;

	public BlueprintEntity(LuaTable entityLua) {
		this.lua = entityLua;
		LuaTable positionLua = entityLua.get("position").checktable();

		this.name = entityLua.get("name").toString();
		this.position = new Point2D.Double(positionLua.get("x").todouble(), positionLua.get("y").todouble());
		this.direction = Direction.values()[entityLua.get("direction").optint(0)];
	}

	public Direction getDirection() {
		return direction;
	}

	public String getName() {
		return name;
	}

	public Double getPosition() {
		return position;
	}

	public LuaTable lua() {
		return lua;
	}
}
