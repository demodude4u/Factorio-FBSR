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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSRollingStockEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.entity.RailRendering.RailDef;
import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.fp.FPRollingStockRotatedSlopedGraphics;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.map.MapDebug;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRail;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.demod.fbsr.map.MapTintOverrideSprite;

public class RollingStockRendering extends VehicleRendering {

	public static final double ELEVATED_SHADOW_SHIFT_MULTIPLIER = 1.41421356237;

	private FPColor protoColor;
	private double protoJointDistance;
	protected FPRollingStockRotatedSlopedGraphics protoPictures;
	private Optional<FPRollingStockRotatedSlopedGraphics> protoWheels;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSRollingStockEntity bsEntity = entity.fromBlueprint();

		Color color;
		if (bsEntity.color.isPresent()) {
			color = bsEntity.color.get().createColor();
		} else {
			color = protoColor.createColor();
//			color = new FPColor(0.92, 0.07, 0, 0.5).createColor();
		}

		Placement actual = findActualPlacement(map, entity);
		bsEntity.setActual(actual);

		// Calculate wheel positions
		double halfDistance = protoJointDistance / 2.0;
		MapPosition3D wheelsFrontPos = calculateWheelPosition(actual.pos, actual.orientation, actual.slope, halfDistance);
		MapPosition3D wheelsBackPos = calculateWheelPosition(actual.pos, actual.orientation, actual.slope, -halfDistance);

		double orientation = actual.orientation;
		double orientation180 = orientation < 0.5 ? orientation + 0.5 : orientation - 0.5;
		
		// Placement actualWheelsFront = findWheelPlacement(register, map, wheelsFrontPos, orientation);
		// Placement actualWheelsBack = findWheelPlacement(register, map, wheelsBackPos, orientation180);
		Placement actualWheelsFront = new Placement(wheelsFrontPos, orientation, actual.slope);
		Placement actualWheelsBack = new Placement(wheelsBackPos, orientation180, -actual.slope);

		Layer layer = (actual.pos.getHeight() > (RailRendering.ELEVATED_HEIGHT / 2.0)) ? Layer.ELEVATED_HIGHER_OBJECT : Layer.HIGHER_OBJECT_UNDER;
		Consumer<SpriteDef> actualRegister = s -> {
			if (s.applyRuntimeTint()) {
				register.accept(new MapTintOverrideSprite(s, layer, actual.pos.flatten(), color));
			} else if (s.isShadow()) {
				double shift = actual.pos.getHeight() * ELEVATED_SHADOW_SHIFT_MULTIPLIER;
				register.accept(new MapSprite(s, layer, actual.pos.get2D().addUnit(shift, 0)));
			} else {
				register.accept(new MapSprite(s, layer, actual.pos.flatten()));
			}
		};

		if (protoWheels.isPresent()) {
			if (actualWheelsFront.slope == 0) {
				protoWheels.get().rotated.defineSprites(s -> {
					// register.accept(new MapSprite(s, layer, actualWheelsFront.pos.flatten()));
					register.accept(new MapSprite(s, layer, wheelsFrontPos.flatten()));
				}, actualWheelsFront.orientation);

			} else {
				Direction dir = Direction.fromCardinal((int)Math.round(actualWheelsFront.orientation * 4) % 4);
				protoWheels.get().sloped.get().defineSprites(s -> {
					// register.accept(new MapSprite(s, layer, actualWheelsFront.pos.flatten()));
					register.accept(new MapSprite(s, layer, wheelsFrontPos.flatten()));
				}, dir, actualWheelsFront.slope);
			}

			if (actualWheelsBack.slope == 0) {
				protoWheels.get().rotated.defineSprites(s -> {
					// register.accept(new MapSprite(s, layer, actualWheelsBack.pos.flatten()));
					register.accept(new MapSprite(s, layer, wheelsBackPos.flatten()));
				}, actualWheelsBack.orientation);

			} else {
				Direction dir = Direction.fromCardinal((int)Math.round(actualWheelsBack.orientation * 4) % 4);
				protoWheels.get().sloped.get().defineSprites(s -> {
					// register.accept(new MapSprite(s, layer, actualWheelsBack.pos.flatten()));
					register.accept(new MapSprite(s, layer, wheelsBackPos.flatten()));
				}, dir, actualWheelsBack.slope);
			}
		}

		if (actual.slope == 0) {
			protoPictures.rotated.defineSprites(actualRegister, orientation);

		} else {
			Direction dir = Direction.fromCardinal((int)Math.round(orientation * 4) % 4);
			protoPictures.sloped.get().defineSprites(actualRegister, dir, actual.slope);
		}

		// register.accept(new MapRenderable(Layer.DEBUG) {
		// 	@Override
		// 	public void render(Graphics2D g) {
		// 		AffineTransform pt = g.getTransform();
		// 		Stroke ps = g.getStroke();
		// 		g.setStroke(MapDebug.STROKE_BOLD);

		// 		Point2D.Double gp = entity.getPosition().createPoint2D();
		// 		Point2D.Double ap = actual.pos.flatten().createPoint2D();
		// 		Point2D.Double wp1 = actualWheelsFront.pos.flatten().createPoint2D();
		// 		Point2D.Double wp2 = actualWheelsBack.pos.flatten().createPoint2D();
				
		// 		g.setColor(Color.white);
		// 		g.draw(new Line2D.Double(gp, ap));
		// 		g.draw(new Line2D.Double(ap, wp1));
		// 		g.draw(new Line2D.Double(ap, wp2));

		// 		g.setColor(Color.yellow);
		// 		g.draw(new Ellipse2D.Double(gp.x-0.2, gp.y-0.2, 0.4, 0.3));

				
		// 		g.setColor(Color.magenta);
		// 		g.draw(new Ellipse2D.Double(ap.x-0.15, ap.y-0.15, 0.3, 0.225));

		// 		g.setColor(Color.green);
		// 		g.draw(new Ellipse2D.Double(wp1.x-0.1, wp1.y-0.1, 0.2, 0.15));
		// 		g.draw(new Ellipse2D.Double(wp2.x-0.1, wp2.y-0.1, 0.2, 0.15));

		// 		g.setStroke(ps);
		// 		g.setTransform(pt);
		// 	}
		// });
		
	}
	
	private MapPosition3D calculateWheelPosition(MapPosition3D pos, double orientation, double slope, double offsetForward) {
        double rotation = orientation * Math.PI * 2 - Math.PI * 0.5;

        if (slope == 0) {
            double offsetX = offsetForward * Math.cos(rotation); 
            double offsetY = offsetForward * Math.sin(rotation);
            
            return pos.addUnit(offsetX, offsetY, 0);
        }
        
        double slopeAngle = Math.atan(slope);
        double horizontalComponent = offsetForward * Math.cos(slopeAngle);
        double verticalComponent = offsetForward * Math.sin(slopeAngle);
        
        double finalOffsetX = horizontalComponent * Math.cos(rotation);
        double finalOffsetY = horizontalComponent * Math.sin(rotation);
        
        return pos.addUnit(finalOffsetX, finalOffsetY, verticalComponent);
    }

	public static class Placement {
		public final MapPosition3D pos;
		public final double orientation;
		public final double slope;

		public Placement(MapPosition3D pos, double orientation, double slope) {
			this.pos = pos;
			this.orientation = orientation;
			this.slope = slope;
		}

		public static Placement failedPlacement(MapPosition pos, double orientation, double jointDistance) {
			return new Placement(MapPosition3D.by2DGround(pos), orientation, 0);
		}
	}

	private Placement findActualPlacement(WorldMap map, MapEntity entity) {
        MapPosition pos = entity.getPosition();
        double startOrientation = entity.fromBlueprint().orientation.orElse(0);
        double startRotation = startOrientation * Math.PI * 2 - Math.PI * 0.5;
		
        List<MapRail> matchingRails = findRailsContainingPosition(pos, map);
        
        Optional<ClosestPointResult> centerRailResult = findClosestPointOnRails(pos, matchingRails);
        if (centerRailResult.isEmpty()) {
            return Placement.failedPlacement(pos, startOrientation, protoJointDistance);
        }
        MapPosition3D centerRailPos = centerRailResult.get().railPos;
        
        if (centerRailPos.getHeight() > 0 && centerRailPos.getHeight() < RailRendering.ELEVATED_HEIGHT) {
            double halfDistance = protoJointDistance / 2.0;
            MapPosition3D wheelsFrontRailPos = null;
            MapPosition3D wheelsBackRailPos = null;
            
            {
                MapPosition pos1 = calculateWheelPosition(centerRailPos, startOrientation, 0, halfDistance).get2D();
                MapPosition pos2 = calculateWheelPosition(centerRailPos, startOrientation, 0, -halfDistance).get2D();

                Optional<ClosestPointResult> result1 = findClosestPointOnRails(pos1, findRailsContainingPosition(pos1, map));
                Optional<ClosestPointResult> result2 = findClosestPointOnRails(pos2, findRailsContainingPosition(pos2, map));

                if (result1.isPresent() && result2.isPresent()) {
                    wheelsFrontRailPos = result1.get().railPos;
                    wheelsBackRailPos = result2.get().railPos;
                    double h = wheelsFrontRailPos.getHeight() - wheelsBackRailPos.getHeight();
                    double slope = h / protoJointDistance;
					double slopedCenterHeight = (wheelsFrontRailPos.getHeight() + wheelsBackRailPos.getHeight()) / 2.0;
					MapPosition3D slopedCenterRailPos = centerRailPos.addUnit(0, 0, slopedCenterHeight - centerRailPos.getHeight());
					double railShift = 0.25 * Math.abs(Math.cos(startRotation));
					return new Placement(slopedCenterRailPos.addUnit(0, 0, railShift), startOrientation, slope);
                }
            }
        }

		double railShift = 0.25 * Math.abs(Math.cos(startRotation));
		return new Placement(centerRailPos.addUnit(0, 0, railShift), startOrientation, 0);
    }

	private Placement findWheelPlacement(Consumer<MapRenderable> register, WorldMap map, MapPosition3D pos, double orientation) {
		
		double rotation = orientation * Math.PI * 2 - Math.PI * 0.5;
		double halfDistance = 0.25;
		double dx = halfDistance * Math.cos(rotation);
		double dy = halfDistance * Math.sin(rotation);
		
		MapPosition3D pos1 = pos.addUnit(dx, dy, 0);
		MapPosition3D pos2 = pos.addUnit(-dx, -dy, 0);
		
		Optional<ClosestPointResult> result1 = findClosestPointOnRails3D(pos1, findRailsContainingPosition(pos1.get2D(), map));
        Optional<ClosestPointResult> result2 = findClosestPointOnRails3D(pos2, findRailsContainingPosition(pos2.get2D(), map));

		if (result1.isPresent() && result2.isPresent()) {

			MapPosition3D rp1 = result1.get().railPos;
			MapPosition3D rp2 = result2.get().railPos;
			MapPosition3D railPos = MapPosition3D.average(rp1, rp2);
			
			int dyfp = rp2.get2D().getYFP() - rp1.get2D().getYFP();
			int dxfp = rp2.get2D().getXFP() - rp1.get2D().getXFP();
			double railRotation = Math.atan2(dyfp, dxfp);
			
			double railOrientation = railRotation / (Math.PI * 2) + 0.25;
			if (railOrientation < 0) {
				railOrientation += 1;
			}
			while (railOrientation > 1) {
				railOrientation -= 1;
			}

			// register.accept(new MapRenderable(Layer.DEBUG) {
			// 	@Override
			// 	public void render(Graphics2D g) {
			// 		AffineTransform pt = g.getTransform();
			// 		Stroke ps = g.getStroke();
			// 		g.setStroke(MapDebug.STROKE_BOLD);
	
			// 		Point2D.Double p1 = pos1.flatten().createPoint2D();
			// 		Point2D.Double p2 = pos2.flatten().createPoint2D();
			// 		Point2D.Double ap1 = rp1.flatten().createPoint2D();
			// 		Point2D.Double ap2 = rp2.flatten().createPoint2D();
					
			// 		g.setColor(Color.gray);
			// 		g.draw(new Line2D.Double(p1, ap1));
			// 		g.draw(new Line2D.Double(p2, ap2));
	
			// 		g.setColor(Color.yellow);
			// 		g.draw(new Ellipse2D.Double(p1.x-0.05, p1.y-0.05, 0.1, 0.075));
			// 		g.setColor(Color.yellow.darker());
			// 		g.draw(new Ellipse2D.Double(p2.x-0.05, p2.y-0.05, 0.1, 0.075));

			// 		g.setColor(Color.orange);
			// 		g.draw(new Ellipse2D.Double(ap1.x-0.05, ap1.y-0.05, 0.1, 0.075));
			// 		g.setColor(Color.orange.darker());
			// 		g.draw(new Ellipse2D.Double(ap2.x-0.05, ap2.y-0.05, 0.1, 0.075));
	
			// 		g.setStroke(ps);
			// 		g.setTransform(pt);
			// 	}
			// });
			
			double h = rp2.getHeight() - rp1.getHeight();
			double slope = h / (halfDistance * 2.0);
			double railShift = 0.25 * Math.abs(Math.cos(railRotation));
			return new Placement(railPos.addUnit(0, 0, railShift), railOrientation, slope);
		}

		double railShift = 0.25 * Math.abs(Math.cos(rotation));
		return new Placement(pos.addUnit(0, 0, railShift), orientation, 0);
	}

	private List<MapRail> findRailsContainingPosition(MapPosition pos, WorldMap map) {
		List<MapRail> matchingRails = new ArrayList<>();
		for (MapRail rail : map.getRails()) {
			if (rail.getDef().drawBounds.shift(rail.getPos()).contains(pos)) {
				matchingRails.add(rail);
			}
		}
		return matchingRails;
	}

	private static class ClosestPointResult {
		public final MapPosition3D railPos;
		public final double pointDistance;
		public final MapRail rail;
		public final double railDistance;

		public ClosestPointResult(MapPosition3D railPos, double pointDistance, MapRail rail, double railDistance) {
			this.railPos = railPos;
			this.pointDistance = pointDistance;
			this.rail = rail;
			this.railDistance = railDistance;
		}
	}

	private Optional<ClosestPointResult> findClosestPointOnRails(MapPosition pos, List<MapRail> rails) {
		double minDistance = Double.POSITIVE_INFINITY;
		ClosestPointResult closestResult = null;

		if (rails.isEmpty()) {
			return Optional.empty();
		}

		for (MapRail rail : rails) {
			ClosestPointResult result = findClosestPointOnRail(pos, rail);
			if (result.pointDistance < minDistance) {
				minDistance = result.pointDistance;
				closestResult = result;
			}
		}

		return Optional.of(closestResult);
	}

	private ClosestPointResult findClosestPointOnRail(MapPosition pos, MapRail rail) {
		pos = pos.subtract(rail.getPos());

		RailDef railDef = rail.getDef();
		double left = 0;
		double right = railDef.length;
		final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
		final double TOLERANCE = 0.01;

		while (right - left > TOLERANCE) {
			double x1 = right - (right - left) * GOLDEN_RATIO;
			double x2 = left + (right - left) * GOLDEN_RATIO;

			MapPosition3D pos1 = railDef.spliner.splinePoint(railDef, x1);
			MapPosition3D pos2 = railDef.spliner.splinePoint(railDef, x2);

			double dist1 = pos1.get2D().distance(pos);
			double dist2 = pos2.get2D().distance(pos);

			if (dist1 < dist2) {
				right = x2;
			} else {
				left = x1;
			}
		}

		double finalPoint = (left + right) / 2;
		MapPosition3D railPos = railDef.spliner.splinePoint(railDef, finalPoint);
		double pointDistance = railPos.get2D().distance(pos);

		return new ClosestPointResult(railPos.add2D(rail.getPos()), pointDistance, rail, finalPoint);
	}

	private Optional<ClosestPointResult> findClosestPointOnRails3D(MapPosition3D pos, List<MapRail> rails) {
		double minDistance = Double.POSITIVE_INFINITY;
		ClosestPointResult closestResult = null;

		if (rails.isEmpty()) {
			return Optional.empty();
		}

		for (MapRail rail : rails) {
			ClosestPointResult result = findClosestPointOnRail3D(pos, rail);
			if (result.pointDistance < minDistance) {
				minDistance = result.pointDistance;
				closestResult = result;
			}
		}

		return Optional.of(closestResult);
	}

	private ClosestPointResult findClosestPointOnRail3D(MapPosition3D pos, MapRail rail) {
		pos = pos.subtract2D(rail.getPos());

		RailDef railDef = rail.getDef();
		double left = 0;
		double right = railDef.length;
		final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
		final double TOLERANCE = 0.01;

		while (right - left > TOLERANCE) {
			double x1 = right - (right - left) * GOLDEN_RATIO;
			double x2 = left + (right - left) * GOLDEN_RATIO;

			MapPosition3D pos1 = railDef.spliner.splinePoint(railDef, x1);
			MapPosition3D pos2 = railDef.spliner.splinePoint(railDef, x2);

			double dist1 = pos1.distance(pos);
			double dist2 = pos2.distance(pos);

			if (dist1 < dist2) {
				right = x2;
			} else {
				left = x1;
			}
		}

		double finalPoint = (left + right) / 2;
		MapPosition3D railPos = railDef.spliner.splinePoint(railDef, finalPoint);
		double pointDistance = railPos.distance(pos);

		return new ClosestPointResult(railPos.add2D(rail.getPos()), pointDistance, rail, finalPoint);
	}

	@Override
	public MapRect3D getDrawBounds(MapEntity entity) {
		return drawBounds.rotateOrientation(entity.fromBlueprint().orientation.orElse(0), true)
				.shift(entity.getPosition());
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPictures.rotated.getDefs(register);
		protoPictures.sloped.ifPresent(fp -> fp.getDefs(register));
		if (protoWheels.isPresent()) {
			protoWheels.get().rotated.getDefs(register);
			protoWheels.get().sloped.ifPresent(fp -> fp.getDefs(register));
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();
		
		protoColor = new FPColor(prototype.lua().get("color"));
		protoJointDistance = prototype.lua().get("joint_distance").todouble();
		protoPictures = new FPRollingStockRotatedSlopedGraphics(profile, prototype.lua().get("pictures"));
		protoWheels = FPUtils.opt(profile, prototype.lua().get("wheels"), FPRollingStockRotatedSlopedGraphics::new);
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSRollingStockEntity.class;
	}
}
