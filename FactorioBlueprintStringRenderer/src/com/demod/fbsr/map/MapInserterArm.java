package com.demod.fbsr.map;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.SpriteDef;

public class MapInserterArm extends MapRenderable {

	private final List<SpriteDef> sprites;
	private final MapPosition pos;
	private final Direction dir;
	private final double armStretch;

	// TODO avoid a sprite list and use the consumer model
	public MapInserterArm(List<SpriteDef> sprites, MapPosition pos, Direction dir, double armStretch) {
		super(Layer.HIGHER_OBJECT_UNDER);

		this.sprites = sprites;
		this.pos = pos;
		this.dir = dir;
		this.armStretch = armStretch;
	}

	@Override
	public void render(Graphics2D g) {
		AffineTransform pat = g.getTransform();

		for (SpriteDef sprite : sprites) {
			MapRect bounds = sprite.getTrimmedBounds();
			Rectangle source = sprite.getAtlasRef().getRect();
			BufferedImage image = sprite.getAtlasRef().getAtlas().getBufferedImage();

			g.translate(pos.xfp, pos.yfp);
			g.rotate(dir.ordinal() * Math.PI / 4.0);
			g.translate(bounds.getX(), 0);
			g.scale(bounds.getWidth(), armStretch);
			g.drawImage(image, 0, 1, 1, 0, source.x, source.y, source.x + source.width, source.y + source.height, null);

			g.setTransform(pat);
		}
	}

}
