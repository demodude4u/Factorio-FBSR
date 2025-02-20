package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;

public class MapModuleIcons extends MapRenderable {

	public MapModuleIcons(MapPosition pos,MapRect)

	@Override
	public void render(Graphics2D g) {
		Point2D.Double pos = entity.position.createPoint();
		Rectangle2D.Double box = protoSelectionBox.createRect();

		double startX = pos.x + box.x + box.width / 2.0 - 0.7 * (renderModules.size() / 2.0) + 0.35;
		double startY = pos.y + box.y + box.height - 0.7;

		Rectangle2D.Double shadowBox = new Rectangle2D.Double(startX - 0.3, startY - 0.3, 0.6, 0.6);
		Rectangle2D.Double spriteBox = new Rectangle2D.Double(startX - 0.25, startY - 0.25, 0.5, 0.5);

		for (String itemName : renderModules) {
			g.setColor(new Color(0, 0, 0, 180));
			g.fill(shadowBox);
			BufferedImage image = data.getTable().getItem(itemName).map(data::getWikiIcon)
					.orElse(RenderUtils.EMPTY_IMAGE);
			RenderUtils.drawImageInBounds(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()), spriteBox,
					g);

			shadowBox.x += 0.7;
			spriteBox.x += 0.7;
		}
	}

}
