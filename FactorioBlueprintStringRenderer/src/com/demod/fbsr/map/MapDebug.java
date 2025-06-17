package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Map.Entry;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.entity.RailRampRendering;
import com.demod.fbsr.entity.RailRendering;
import com.demod.fbsr.entity.RailRendering.RailDef;
import com.demod.fbsr.gui.GUIStyle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

public class MapDebug extends MapRenderable {

	public static final BasicStroke STROKE_THIN = new BasicStroke(1f / (float) FBSR.TILE_SIZE);
	public static final BasicStroke STROKE_BOLD = new BasicStroke(3f / (float) FBSR.TILE_SIZE, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);

	private static final Font FONT_PATH_RAIL = FBSR.getGuiStyle().FONT_BP_BOLD.deriveFont(0.4f);

	private static final Color COLOR_PATH_ITEM = Color.cyan;
	private static final Color COLOR_PATH_RAIL = Color.lightGray;
	private static final Color COLOR_PATH_RAIL_END_POINTS = Color.white;
	private static final Color COLOR_PATH_RAIL_OUTGOING = Color.magenta;
	private static final Color COLOR_PATH_RAIL_INCOMING = Color.cyan;
	private static final Color COLOR_ENTITY_PLACEMENT = Color.magenta;
	private static final Color COLOR_TILE_PLACEMENT = Color.yellow;

	public static final Path2D.Double SHAPE_DIRECTION_MARKER = new Path2D.Double();
	static {
		SHAPE_DIRECTION_MARKER.moveTo(-0.1, 0);
		SHAPE_DIRECTION_MARKER.lineTo(0, -0.5);
		SHAPE_DIRECTION_MARKER.lineTo(0.1, 0);
		SHAPE_DIRECTION_MARKER.closePath();
	}

	private final RenderRequest.Debug debug;
	private final WorldMap map;
	private final List<MapEntity> entities;
	private final List<MapTile> tiles;

	public MapDebug(RenderRequest.Debug debug, WorldMap map, List<MapEntity> entities, List<MapTile> tiles) {
		super(Layer.DEBUG);
		this.debug = debug;
		this.map = map;
		this.entities = entities;
		this.tiles = tiles;
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();

		if (debug.pathRails) {
			renderPathRails(g);
		}

		if (debug.pathItems) {
			renderPathItems(g);
		}

		if (debug.entityPlacement) {
			for (MapTile tile : tiles) {
				renderTilePlacement(g, tile);
			}
			for (MapEntity entity : entities) {
				renderEntityPlacement(g, entity);
			}
		}

		g.setStroke(ps);
	}

	private void renderEntityPlacement(Graphics2D g, MapEntity entity) {
		MapPosition position = entity.getPosition();
		MapRect3D bounds = entity.getBounds();
		Direction direction = entity.getDirection();
		OptionalDouble orientation = entity.fromBlueprint().orientation;
		double x = position.getX();
		double y = position.getY();

		AffineTransform pat = g.getTransform();

		g.setStroke(STROKE_THIN);
		g.setColor(COLOR_ENTITY_PLACEMENT);
		g.draw(new Rectangle2D.Double(bounds.getX1(), bounds.getY1() - bounds.getHeight(),
				bounds.getX2() - bounds.getX1(), bounds.getY2() - bounds.getY1() + bounds.getHeight()));
		if (bounds.getHeight() > 0) {
			g.setColor(g.getColor().darker());
			g.draw(new Line2D.Double(bounds.getX1(), bounds.getY1(), bounds.getX2(), bounds.getY1()));
		}
		g.fill(new Ellipse2D.Double(x - 0.1, y - 0.1, 0.2, 0.2));

		g.translate(x, y);
		if (orientation.isPresent()) {
			g.setColor(g.getColor().brighter());
			g.rotate(orientation.getAsDouble() * Math.PI * 2.0);

		} else {
			g.rotate(direction.ordinal() / (Math.PI / 4.0));
		}
		g.fill(SHAPE_DIRECTION_MARKER);
		g.setTransform(pat);

		// Show direction number
		// g.setColor(Color.white);
		// g.setFont(GUIStyle.FONT_BP_BOLD.deriveFont(0.5f));
		// g.drawString("" + entity.getDirection().ordinal(), (float) position.getX(), (float) position.getY());

		// Show entity name
		g.setColor(Color.white);
		g.setFont(FBSR.getGuiStyle().FONT_BP_BOLD.deriveFont(0.25f));
		g.drawString("" + entity.fromBlueprint().name, (float) position.getX(), (float) position.getY() + 0.5f);
	}

	private void renderPathItems(Graphics2D g) {
		g.setStroke(STROKE_BOLD);
		g.setColor(COLOR_PATH_ITEM);
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		logisticGrid.cellSet().forEach(c -> {
			MapPosition pos = MapPosition.byUnit(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getMovedFrom().ifPresent(l -> {
				for (Direction d : l) {
					MapPosition p = d.offset(pos, 0.5);
					g.draw(new Line2D.Double(pos.createPoint2D(), p.createPoint2D()));
				}
			});
		});
	}

	private void renderPathRails(Graphics2D g) {
		AffineTransform pat = g.getTransform();

		// for (Entry<RailEdge, RailEdge> pair : map.getRailEdges()) {
		// 	for (RailEdge edge : ImmutableList.of(pair.getKey(), pair.getValue())) {
		// 		if (edge.isBlocked()) {
		// 			continue;
		// 		}

		// 		RailDirection d1 = edge.getStart().dir;
		// 		MapPosition p1 = d1.right().offset(edge.getStart().pos, 0.25);
		// 		RailDirection d2 = edge.getEnd().dir;
		// 		MapPosition p2 = d2.back().right().offset(edge.getEnd().pos, 0.25);

		// 		g.draw(new Line2D.Double(p1.createPoint2D(), p2.createPoint2D()));

		// 		g.translate(p1.getX(), p1.getY());
		// 		g.rotate(d1.getAngle());
		// 		g.fill(SHAPE_DIRECTION_MARKER);
		// 		g.setTransform(pat);

		// 		g.translate(p2.getX(), p2.getY());
		// 		g.rotate(d2.getAngle());
		// 		g.fill(SHAPE_DIRECTION_MARKER);
		// 		g.setTransform(pat);
		// 	}
		// }

		g.setStroke(STROKE_BOLD);
		for (MapRail rail : map.getRails()) {

			MapPosition pos = rail.getPos();
			RailDef def = rail.getDef();

			Dir16 d1 = def.A.dir;
			MapPosition p1 = pos.add(def.A.pos);
			if (def.A.elevated) {
				p1 = p1.addUnit(0, -RailRendering.ELEVATED_HEIGHT);
			}
			Dir16 d2 = def.B.dir;
			MapPosition p2 = pos.add(def.B.pos);
			if (def.B.elevated) {
				p2 = p2.addUnit(0, -RailRendering.ELEVATED_HEIGHT);
			}

			// g.draw(new Line2D.Double(p1.createPoint2D(), p2.createPoint2D()));

			g.setColor(COLOR_PATH_RAIL_END_POINTS);
			g.translate(p1.getX(), p1.getY());
			g.rotate(d1.back().getAngle());
			g.fill(SHAPE_DIRECTION_MARKER);
			g.setTransform(pat);

			g.translate(p2.getX(), p2.getY());
			g.rotate(d2.back().getAngle());
			g.fill(SHAPE_DIRECTION_MARKER);
			g.setTransform(pat);

			g.setColor(COLOR_PATH_RAIL);
			g.translate(pos.getX(), pos.getY());
			g.draw(def.drawPath);
			g.setTransform(pat);
		}

//		g.setStroke(STROKE_THIN);
//		g.setFont(FONT_PATH_RAIL);
//		map.getRailNodes().cellSet().forEach(c -> {
//			MapPosition pos = MapPosition.byUnit(c.getRowKey() / 2.0, c.getColumnKey() / 2.0);
//			RailNode node = c.getValue();
//
//			g.setColor(COLOR_PATH_RAIL_INCOMING);
//			for (Direction dir : Direction.values()) {
//				Collection<RailEdge> edges = node.getIncomingEdges(dir);
//				if (!edges.isEmpty()) {
//					MapPosition p1 = dir.right().offset(pos, 0.25);
//					MapPosition p2 = dir.offset(p1, 0.5);
//					g.draw(new Line2D.Double(p1.createPoint2D(), p2.createPoint2D()));
//					g.drawString("" + edges.size(), (float) p2.getX() - 0.1f, (float) p2.getY() - 0.2f);
//				}
//			}
//
//			g.setColor(COLOR_PATH_RAIL_OUTGOING);
//			for (Direction dir : Direction.values()) {
//				Collection<RailEdge> edges = node.getOutgoingEdges(dir);
//				if (!edges.isEmpty()) {
//					MapPosition p1 = dir.left().offset(pos, 0.25);
//					MapPosition p2 = dir.offset(p1, 0.5);
//					g.draw(new Line2D.Double(p1.createPoint2D(), p2.createPoint2D()));
//					g.drawString("" + edges.size(), (float) p2.getX() - 0.1f, (float) p2.getY() - 0.2f);
//				}
//			}
//		});
	}

	private void renderTilePlacement(Graphics2D g, MapTile tile) {
		MapPosition position = tile.getPosition();
		double x = position.getX();
		double y = position.getY();

		g.setStroke(STROKE_THIN);
		g.setColor(COLOR_TILE_PLACEMENT);
		g.draw(new Rectangle2D.Double(x - 0.5, y - 0.5, 1.0, 1.0));
		g.fill(new Ellipse2D.Double(x - 0.1, y - 0.1, 0.2, 0.2));
	}

}
