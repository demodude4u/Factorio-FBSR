package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.Atlas;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.Atlas.AtlasRef;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.IconDef;
import com.google.common.collect.Table;

public class MapItemLogistics extends MapRenderable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapItemLogistics.class);

	private static final Map<String, Color> itemColorCache = new HashMap<>();

	private static synchronized Color getItemLogisticColor(String itemName) {
		return itemColorCache.computeIfAbsent(itemName, k -> {
			Optional<IconDef> icon = FBSR.getIconManager().lookupItem(k);
			if (!icon.isPresent()) {
				LOGGER.warn("ITEM MISSING FOR LOGISTICS: {}", k);
				return Color.MAGENTA;
			}
			AtlasRef ref = icon.get().getAtlasRef();
			Color color = RenderUtils.getAverageColor(ref.getAtlas().getImage(), ref.getRect());
			float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
			return Color.getHSBColor(hsb[0], Math.max(0.25f, hsb[1]), Math.max(0.5f, hsb[2]));
		});
	}

	private final WorldMap map;

	public MapItemLogistics(WorldMap map) {
		super(Layer.LOGISTICS_MOVE);
		this.map = map;
	}

	@Override
	public void render(Graphics2D g) {
		// TODO redo this logic for more performance
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		logisticGrid.cellSet().forEach(c -> {
			MapPosition pos = MapPosition.byUnit(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getTransits().ifPresent(s -> {
				if (s.isEmpty()) {
					return;
				}
				int i = 0;
				float width = 0.3f / s.size();
				Stroke ps = g.getStroke();
				g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				for (String itemName : s) {
					double shift = ((i + 1) / (double) (s.size() + 1) - 0.5) / 3.0; // -0.25..0.25
					cell.getMove().filter(d -> map.getLogisticGridCell(d.offset(pos, 0.5))
							.map(LogisticGridCell::isAccepting).orElse(false)).ifPresent(d -> {
								g.setColor(RenderUtils.withAlpha(getItemLogisticColor(itemName), 255 - 127 / s.size()));
								g.draw(new Line2D.Double(d.right().offset(pos, shift).createPoint2D(),
										d.right().offset(d.offset(pos, 0.5), shift).createPoint2D()));
							});
					i++;
				}
				g.setStroke(ps);
			});
		});
	}

}
