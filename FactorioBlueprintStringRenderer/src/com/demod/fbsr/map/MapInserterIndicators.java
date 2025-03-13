package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.SpriteDef;

public class MapInserterIndicators extends MapRenderable {

	private final List<SpriteDef> lineSprites;
	private final List<SpriteDef> arrowSprites;
	private final MapPosition pos;
	private final MapPosition inPos;
	private final MapPosition outPos;
	private final MapPosition pickupPos;
	private final MapPosition insertPos;
	private final Direction dir;
	private final boolean modded;

	// TODO avoid a sprite list and use the consumer model
	public MapInserterIndicators(List<SpriteDef> lineSprites, List<SpriteDef> arrowSprites, MapPosition pos,
			MapPosition inPos, MapPosition outPos, MapPosition pickupPos, MapPosition insertPos, Direction dir,
			boolean modded) {
		super(Layer.ENTITY_INFO_ICON_ABOVE);

		this.lineSprites = lineSprites;
		this.arrowSprites = arrowSprites;
		this.pos = pos;
		this.inPos = inPos;
		this.outPos = outPos;
		this.pickupPos = pickupPos;
		this.insertPos = insertPos;
		this.dir = dir;
		this.modded = modded;
	}

	@Override
	public void render(Graphics2D g) {
		AffineTransform pat = g.getTransform();

		double pickupRotate = Math.atan2(pickupPos.yfp, pickupPos.xfp);

		for (SpriteDef sprite : lineSprites) {
			MapRect bounds = sprite.getTrimmedBounds();
			Rectangle source = sprite.getAtlasRef().getRect();
			BufferedImage image = sprite.getAtlasRef().getAtlas().getBufferedImage();

			if (modded) {
				g.setTransform(pat);
				g.setColor(RenderUtils.withAlpha(Color.yellow, 64));
				g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(new Line2D.Double(pos.createPoint2D(), inPos.createPoint2D()));
			}

			g.setTransform(pat);
			g.translate(inPos.getX(), inPos.getY());
			// HACK magic numbers
			Point2D.Double magicImageShift = new Point2D.Double(bounds.getX() + 0.1, bounds.getY() + -0.05);
			g.translate(magicImageShift.x, magicImageShift.y);
			if (modded) {
				g.translate(-Math.cos(pickupRotate) * 0.2, -Math.sin(pickupRotate) * 0.2);
				g.rotate(pickupRotate + Math.PI / 2.0, -magicImageShift.x, -magicImageShift.y);
			} else {
				g.rotate(dir.back().ordinal() * Math.PI / 4.0, -magicImageShift.x, -magicImageShift.y);
			}
			// magic numbers from Factorio code
			g.scale(0.8, 0.8);
			g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

			g.setTransform(pat);
		}

		double insertRotate = Math.atan2(insertPos.yfp, insertPos.xfp);

		for (SpriteDef sprite : arrowSprites) {
			MapRect bounds = sprite.getTrimmedBounds();
			Rectangle source = sprite.getAtlasRef().getRect();
			BufferedImage image = sprite.getAtlasRef().getAtlas().getBufferedImage();

			if (modded) {
				g.setTransform(pat);
				g.setColor(RenderUtils.withAlpha(Color.yellow, 64));
				g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(new Line2D.Double(pos.createPoint2D(), outPos.createPoint2D()));
			}

			g.setTransform(pat);
			g.translate(outPos.getX(), outPos.getY());
			// HACK magic numbers
			Point2D.Double magicImageShift = new Point2D.Double(bounds.getX() + 0.1, bounds.getY() + 0.35);
			g.translate(magicImageShift.x, magicImageShift.y);
			if (modded) {
				g.translate(Math.cos(insertRotate) * 0.2, Math.sin(insertRotate) * 0.2);
				g.rotate(insertRotate + Math.PI / 2.0, -magicImageShift.x, -magicImageShift.y);
			} else {
				g.rotate(dir.back().ordinal() * Math.PI / 4.0, -magicImageShift.x, -magicImageShift.y);
			}
			// magic numbers from Factorio code
			g.scale(0.8, 0.8);
			g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

			g.setTransform(pat);
		}
	}

}
