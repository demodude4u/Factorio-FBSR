package com.demod.fbsr.map;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

import com.demod.fbsr.AtlasRef;
import com.demod.fbsr.BlendMode;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.composite.TintComposite;
import com.demod.fbsr.composite.TintOverlayComposite;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPCranePart;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.fp.FPVector3D;
import com.demod.fbsr.gui.GUIStyle;

public class MapCranePart extends MapRenderable implements MapBounded {

    private SpriteDef def;
    private MapPosition pos;
    private double orientation;
    private FPCranePart part;
    private double scaleHeight;
    private double crop;

    public MapCranePart(SpriteDef def, MapPosition pos, double orientation, double scaleHeight, double crop, FPCranePart part) {
        super(def.isShadow() ? Layer.SHADOW_BUFFER : part.layer);
        this.def = def;
        this.pos = pos;
        this.orientation = orientation;
        this.scaleHeight = scaleHeight;
        this.crop = crop;
        this.part = part;
    }

    @Override
    public void render(Graphics2D g) {
        AtlasRef ref = def.getAtlasRef();
		Image image = def.requestAtlas();
		Rectangle source = ref.getRect();

		Composite pc = g.getComposite();
		AffineTransform pat = g.getTransform();

		BlendMode blendMode = def.getBlendMode();
		Optional<Color> tint = def.getTint();
		// TODO tint with additive blending
		if (tint.isPresent() && blendMode == BlendMode.NORMAL) {
			if (def.isTintAsOverlay()) {
				g.setComposite(new TintOverlayComposite(tint.get()));
			} else {
				g.setComposite(new TintComposite(tint.get()));
			}

		} else {
			g.setComposite(blendMode.getComposite());
		}

        MapRect bounds = def.getTrimmedBounds();
        
        g.translate(pos.getX(), pos.getY());
        g.rotate(orientation * Math.PI * 2);
        g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth(), bounds.getHeight() * scaleHeight);
        
        Shape pclip = g.getClip();
        if (crop > 0) {
            g.clip(new Rectangle2D.Double(0, crop, 1, 1 - crop));
        }

        g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

        if (crop > 0) {
            g.setClip(pclip);
        }

		g.setTransform(pat);
		g.setComposite(pc);
    }

    @Override
    public MapRect getBounds() {
        return def.getTrimmedBounds().rotate(orientation * Math.PI * 2).add(pos);
    }

}
