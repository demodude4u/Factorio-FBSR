package com.demod.fbsr;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Quadtree {
	private static final int MAX_OBJECTS = 10; // Max rectangles before splitting
	private static final int MAX_LEVELS = 5; // Max depth of recursion

	private final int level;
	private final Rectangle bounds;

	private List<Rectangle> objects;
	private Quadtree[] nodes;

	public Quadtree(int level, Rectangle bounds) {
		this.level = level;
		this.bounds = bounds;
		this.objects = new ArrayList<>();
		this.nodes = null;
	}

	private void split() {
		int subWidth = bounds.width / 2;
		int subHeight = bounds.height / 2;
		int x = bounds.x;
		int y = bounds.y;

		nodes = new Quadtree[4];
		nodes[0] = new Quadtree(level + 1, new Rectangle(x + subWidth, y, subWidth, subHeight)); // Top-right
		nodes[1] = new Quadtree(level + 1, new Rectangle(x, y, subWidth, subHeight)); // Top-left
		nodes[2] = new Quadtree(level + 1, new Rectangle(x, y + subHeight, subWidth, subHeight)); // Bottom-left
		nodes[3] = new Quadtree(level + 1, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight)); // Bottom-right

		for (Rectangle rect : objects) {
			for (Quadtree node : nodes) {
				node.insertIfNoCollision(rect);
			}
		}
		objects = null;
	}

	public Rectangle insertIfNoCollision(Rectangle rect) {
		if (!rect.intersects(bounds)) {
			return null;
		}

		if (nodes != null) {
			for (Quadtree node : nodes) {
				Rectangle collision = node.insertIfNoCollision(rect);
				if (collision != null) {
					return collision;
				}
			}

		} else {
			for (Rectangle r : objects) {
				if (r.intersects(rect)) {
					return r;
				}
			}

			objects.add(rect);

			if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
				split();
			}
		}

		return null;
	}

}
