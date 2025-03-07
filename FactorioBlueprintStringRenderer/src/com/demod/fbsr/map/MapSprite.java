package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Optional;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.TintComposite;

public class MapSprite extends MapRenderable {

	protected final SpriteDef def;
	protected final MapRect bounds;

	public MapSprite(LayeredSpriteDef def, MapPosition pos) {
		this(def, def.getLayer(), pos);
	}

	public MapSprite(SpriteDef def, Layer layer, MapPosition pos) {
		super(def.isShadow() ? Layer.SHADOW_BUFFER : layer);
		this.def = def;
		this.bounds = def.getTrimmedBounds().add(pos);
	}

	public MapRect getBounds() {
		return bounds;
	}

	public SpriteDef getDef() {
		return def;
	}

	@Override
	public void render(Graphics2D g) {
		AtlasRef ref = def.getAtlasRef();
		if (!ref.isValid()) {
			throw new IllegalStateException("Sprite not assigned to atlas! " + def.getPath());
		}
		Image image = ref.getAtlas().getBufferedImage();
		Rectangle source = ref.getRect();

		Composite pc = g.getComposite();
		AffineTransform pat = g.getTransform();

		Optional<Color> tint = def.getTint().map(this::tintOverride);
		// TODO tint with blending
		if (tint.isPresent()) {
			g.setComposite(new TintComposite(tint.get()));

		} else {
			g.setComposite(def.getBlendMode().getComposite());
		}

		// TODO change the approach to eliminate transforming on every sprite
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

		g.setTransform(pat);
		g.setComposite(pc);
	}

	protected Color tintOverride(Color tint) {
		return tint;
	}
}
