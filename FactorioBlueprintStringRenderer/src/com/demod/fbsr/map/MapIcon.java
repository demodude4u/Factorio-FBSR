package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;
import java.util.OptionalDouble;

import com.demod.fbsr.Atlas;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.Atlas.AtlasRef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;

public class MapIcon extends MapRenderable {
	private static final Color BACKGROUND = new Color(0, 0, 0, 180);

	private final MapPosition position;
	private final ImageDef image;
	private final double size;
	private final OptionalDouble border;
	private final Optional<String> quality;
	private final ModdingResolver resolver;

	public MapIcon(MapPosition position, ImageDef image, double size, OptionalDouble border, boolean above,
			Optional<String> quality, ModdingResolver resolver) {
		super(above ? Layer.ENTITY_INFO_ICON_ABOVE : Layer.ENTITY_INFO_ICON);
		this.position = position;
		this.image = image;
		this.size = size;
		this.border = border;
		this.quality = quality;
		this.resolver = resolver;
	}

	@Override
	public void render(Graphics2D g) {
		double x = position.getX();
		double y = position.getY();
		double halfSize = size / 2.0;

		if (border.isPresent()) {
			double b = border.getAsDouble();
			double shadowSize = size + b * 2.0;
			double halfShadowSize = shadowSize / 2.0;
			g.setColor(BACKGROUND);
			g.fill(new RoundRectangle2D.Double(x - halfShadowSize, y - halfShadowSize, shadowSize, shadowSize, b * 2,
					b * 2));
		}

		AtlasRef ref = image.getAtlasRef();
		if (!ref.isValid()) {
			throw new IllegalStateException("Icon not assigned to atlas! " + image.getPath());
		}
		Image image = ref.getAtlas().getImage();
		Rectangle source = ref.getRect();

		AffineTransform pat = g.getTransform();

		MapRect bounds = MapRect.byUnit(x - halfSize, y - halfSize, size, size);
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight());
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

		if (quality.isPresent()) {
			Optional<IconDef> def = resolver.resolveIconQualityName(quality.get());
			double qSize = 0.4;
			g.translate(0, 1.0 - qSize);
			g.scale(qSize, qSize);
			AtlasRef qRef = def.get().getAtlasRef();
			Image qImage = qRef.getAtlas().getImage();
			Rectangle qSource = qRef.getRect();
			g.drawImage(qImage, 0, 0, 1, 1, qSource.x, qSource.y, qSource.x + qSource.width, qSource.y + qSource.height,
					null);
		}

		g.setTransform(pat);
	}

}
