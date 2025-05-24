package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.entity.BSRollingStockEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPRollingStockRotatedSlopedGraphics;
import com.demod.fbsr.map.MapDebug;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public class ArtilleryWagonRendering extends RollingStockRendering {

	private FPRollingStockRotatedSlopedGraphics protoCannonBarrelPictures;
	private FPRollingStockRotatedSlopedGraphics protoCannonBasePictures;

	private double protoCannonBaseHeight;
	private double protoCannonBaseShiftWhenVertical;
	private double protoCannonBaseShiftWhenHorizontal;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSRollingStockEntity bsEntity = entity.fromBlueprint();
		Placement actual = bsEntity.getActual();

		double orientation = actual.orientation;

		MapPosition3D cannonBasePos = calculateCannonPosition(actual.pos, actual.orientation, actual.slope);

		Layer layer = (cannonBasePos.getHeight() > (RailRendering.ELEVATED_HEIGHT / 2.0))
				? Layer.ELEVATED_HIGHER_OBJECT
				: Layer.HIGHER_OBJECT_UNDER;
		Consumer<SpriteDef> cannonRegister = s -> register.accept(new MapSprite(s, layer, cannonBasePos.flatten()));
			
		if (actual.slope == 0) {
			protoCannonBarrelPictures.rotated.defineSprites(cannonRegister, orientation);
			protoCannonBasePictures.rotated.defineSprites(cannonRegister, orientation);
			
		} else {
			Direction dir = Direction.fromCardinal((int)Math.round(orientation * 4) % 4);
			protoCannonBarrelPictures.sloped.get().defineSprites(cannonRegister, dir, actual.slope);
			protoCannonBasePictures.sloped.get().defineSprites(cannonRegister, dir, actual.slope);
		}

		// register.accept(new MapRenderable(Layer.DEBUG) {
		// 	@Override
		// 	public void render(Graphics2D g) {
		// 		AffineTransform pt = g.getTransform();
		// 		Stroke ps = g.getStroke();
		// 		g.setStroke(MapDebug.STROKE_BOLD);

		// 		Point2D.Double ap = actual.pos.flatten().createPoint2D();
		// 		Point2D.Double cbp = cannonBasePos.flatten().createPoint2D();
				
		// 		g.setColor(Color.cyan);
		// 		g.draw(new Line2D.Double(ap, cbp));

		// 		g.setColor(Color.blue);
		// 		g.draw(new Ellipse2D.Double(cbp.x-0.1, cbp.y-0.1, 0.2, 0.15));

		// 		g.setStroke(ps);
		// 		g.setTransform(pt);
		// 	}
		// });
	}

	protected MapPosition3D calculateCannonPosition(MapPosition3D pos, double orientation, double slope) {
        double rotation = orientation * Math.PI * 2 + Math.PI * 0.5;

		double aligned = protoPictures.rotated.getAlignedOrientation(orientation);
		double vf = Math.abs(Math.abs(aligned - 0.5) - 0.25) * 4.0;
		double offsetForward = -(protoCannonBaseShiftWhenVertical * vf + protoCannonBaseShiftWhenHorizontal * (1.0 - vf));
		double offsetHeight = protoCannonBaseHeight;

        if (slope == 0) {
            double offsetX = offsetForward * Math.cos(rotation); 
            double offsetY = offsetForward * Math.sin(rotation);
            
            return pos.addUnit(offsetX, offsetY * FPUtils.PROJECTION_CONSTANT, offsetHeight * FPUtils.PROJECTION_CONSTANT);
		}
        
        double slopeAngle = Math.atan(-slope);
        double horizontalComponent = offsetForward * Math.cos(slopeAngle) + offsetHeight * -Math.sin(slopeAngle);
        double verticalComponent = offsetForward * Math.sin(slopeAngle) + offsetHeight * Math.cos(slopeAngle);
        
        double finalOffsetX = horizontalComponent * Math.cos(rotation);
        double finalOffsetY = horizontalComponent * Math.sin(rotation);
        
		return pos.addUnit(finalOffsetX, finalOffsetY * FPUtils.PROJECTION_CONSTANT, verticalComponent * FPUtils.PROJECTION_CONSTANT);
    }

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoCannonBarrelPictures.rotated.getDefs(register);
		protoCannonBarrelPictures.sloped.ifPresent(fp -> fp.getDefs(register));

		protoCannonBasePictures.rotated.getDefs(register);
		protoCannonBasePictures.sloped.ifPresent(fp -> fp.getDefs(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoCannonBarrelPictures = new FPRollingStockRotatedSlopedGraphics(profile, 
				prototype.lua().get("cannon_barrel_pictures"));
		protoCannonBasePictures = new FPRollingStockRotatedSlopedGraphics(profile, prototype.lua().get("cannon_base_pictures"));

		protoCannonBaseHeight = prototype.lua().get("cannon_base_height").optdouble(0.0);
		protoCannonBaseShiftWhenVertical = prototype.lua().get("cannon_base_shift_when_vertical").optdouble(0.0);
		protoCannonBaseShiftWhenHorizontal = prototype.lua().get("cannon_base_shift_when_horizontal").optdouble(0.0);
	}
}
