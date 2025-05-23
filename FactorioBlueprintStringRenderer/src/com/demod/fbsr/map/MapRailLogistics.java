package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.util.Map.Entry;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WorldMap;

public class MapRailLogistics extends MapRenderable {

	private static final BasicStroke STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);

	private static final Color COLOR_IO = RenderUtils.withAlpha(Color.yellow, 32);
	private static final Color COLOR_I = RenderUtils.withAlpha(Color.green, 32);
	private static final Color COLOR_O = RenderUtils.withAlpha(Color.red, 32);

	private final WorldMap map;

	public MapRailLogistics(WorldMap map) {
		super(Layer.LOGISTICS_RAIL_IO);
		this.map = map;
	}

	@Override
	public void render(Graphics2D g) {
		Stroke ps = g.getStroke();
		g.setStroke(STROKE);
		// for (Entry<RailEdge, RailEdge> pair : map.getRailEdges()) {
		// 	boolean input = pair.getKey().isInput() || pair.getValue().isInput();
		// 	boolean output = pair.getKey().isOutput() || pair.getValue().isOutput();

		// 	if (input || output) {
		// 		RailEdge edge = pair.getKey();
		// 		MapPosition p1 = edge.getStart().pos;
		// 		RailDirection d1 = edge.getStart().dir;
		// 		MapPosition p2 = edge.getEnd().pos;
		// 		RailDirection d2 = edge.getEnd().dir;

		// 		Shape path;
		// 		double control = 1.7;
		// 		MapPosition cc1 = d1.offset(p1, control);
		// 		MapPosition cc2 = d2.offset(p2, control);
		// 		path = new CubicCurve2D.Double(p1.getX(), p1.getY(), cc1.getX(), cc1.getY(), cc2.getX(), cc2.getY(),
		// 				p2.getX(), p2.getY());

		// 		Color color = (input && output) ? COLOR_IO : input ? COLOR_I : COLOR_O;

		// 		g.setColor(color);
		// 		g.draw(path);
		// 	}
		// }
		g.setStroke(ps);
	}

}
