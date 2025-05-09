package com.demod.fbsr.entity;

import java.awt.geom.Path2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSRailEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPRailFenceGraphicsSet;
import com.demod.fbsr.fp.FPRailPictureSet;
import com.demod.fbsr.fp.FPRailPieceLayers;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRail;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.google.common.collect.ImmutableList;

public abstract class RailRendering extends EntityWithOwnerRendering {
	private static final int VARIATION = 0;
	// TODO add variation (or should we ignore variations to save atlas space?)

	public static final double ELEVATED_HEIGHT = 3;

	public static class RailPoint {
		public final MapPosition pos;
		public final Dir16 dir;
		public final boolean elevated;

		public RailPoint(double x, double y, String dir, boolean elevated) {
			this(MapPosition.byUnit(x, y), Dir16.valueOf(dir), elevated);
		}

		public RailPoint(MapPosition pos, Dir16 dir, boolean elevated) {
			this.pos = pos;
			this.dir = dir;
			this.elevated = elevated;
		}

		public RailPoint rotate90() {
			return new RailPoint(pos.rotate90(), dir.right(), elevated);
		}

		public RailPoint flipX() {
			return new RailPoint(pos.flipX(), dir.flipX(), elevated);
		}

		public RailPoint flipY() {
			return new RailPoint(pos.flipY(), dir.flipY(), elevated);
		}
	}

	public static class RailDef {
		public final RailPoint A;
		public final RailPoint B;
		public final List<List<RailPoint>> signals;
		public final List<RailPoint> stations;
		public final RailSpliner spliner;
		
		public final MapRect3D drawBounds;
		public final double length;
		public final Path2D drawPath;

		@SafeVarargs
		public RailDef(double xA, double yA, String dirA, boolean elevatedA, double xB, double yB, String dirB,
				boolean elevatedB, RailSpliner spliner, List<RailPoint>... signals) {
			this(new RailPoint(xA, yA, dirA, elevatedA), new RailPoint(xB, yB, dirB, elevatedB), ImmutableList.of(),
					ImmutableList.copyOf(signals), spliner);
		}

		// Station pieces
		@SafeVarargs
		public RailDef(double xA, double yA, String dirA, boolean elevatedA, double xB, double yB, String dirB,
				boolean elevatedB, RailSpliner spliner, double xS1, double yS1, String dirS1, boolean elevatedS1,
				double xS2, double yS2, String dirS2, boolean elevatedS2, List<RailPoint>... signals) {
			this(new RailPoint(xA, yA, dirA, elevatedA), new RailPoint(xB, yB, dirB, elevatedB), ImmutableList
					.of(new RailPoint(xS1, yS1, dirS1, elevatedS1), new RailPoint(xS2, yS2, dirS2, elevatedS2)),
					ImmutableList.copyOf(signals), spliner);
		}

		public RailDef(RailPoint A, RailPoint B, List<RailPoint> stations, List<List<RailPoint>> signals,
				RailSpliner spliner) {
			this.A = A;
			this.B = B;
			this.stations = stations;
			this.signals = signals;
			this.spliner = spliner;

			if (spliner != null) {
				length = spliner.calculateSplineLength(this);
				drawPath = spliner.generatePath(this, 0.1);
			} else {
				length = 0;
				drawPath = null;
			}

			drawBounds = computeBounds();
		}

		protected MapRect3D computeBounds() {
			MapPosition p1 = A.dir.left().offset(A.pos, 1);
			MapPosition p2 = A.dir.right().offset(A.pos, 1);
			MapPosition p3 = B.dir.left().offset(B.pos, 1);
			MapPosition p4 = B.dir.right().offset(B.pos, 1);
			return MapPosition.enclosingBounds(ImmutableList.of(p1, p2, p3, p4))
					.as3DUnit((A.elevated || B.elevated) ? ELEVATED_HEIGHT : 0);
		}

		public RailDef rotate90() {
			return new RailDef(//
					A.rotate90(), //
					B.rotate90(), //
					stations.stream().map(p -> p.rotate90()).collect(Collectors.toList()), //
					signals.stream().map(l -> l.stream().map(p -> p.rotate90()).collect(Collectors.toList()))
							.collect(Collectors.toList()), //
					spliner);
		}

		public RailDef flipX() {
			return new RailDef(//
					A.flipX(), //
					B.flipX(), //
					stations.stream().map(p -> p.flipX()).collect(Collectors.toList()), //
					signals.stream().map(l -> l.stream().map(p -> p.flipX()).collect(Collectors.toList()))
							.collect(Collectors.toList()), //
					spliner);
		}

		public RailDef flipY() {
			return new RailDef(//
					A.flipY(), //
					B.flipY(), //
					stations.stream().map(p -> p.flipY()).collect(Collectors.toList()), //
					signals.stream().map(l -> l.stream().map(p -> p.flipY()).collect(Collectors.toList()))
							.collect(Collectors.toList()), //
					spliner);
		}

		public RailDef withSpliner(RailSpliner spliner) {
			return new RailDef(A, B, stations, signals, spliner);
		}
	}

	public static abstract class RailSpliner {
		public abstract double calculateSplineLength(RailDef rail);

		public abstract MapPosition3D splinePoint(RailDef rail, double distance);

		public Path2D generatePath(RailDef rail, double step) {
			Path2D path = new Path2D.Double();
			MapPosition3D rp = splinePoint(rail, 0);
			MapPosition pos = rp.get2D();
			path.moveTo(pos.getX(), pos.getY() - rp.getHeight());
			for (double d = 0; d < rail.length; d += step) {
				rp = splinePoint(rail, d);
				pos = rp.get2D();
				path.lineTo(pos.getX(), pos.getY() - rp.getHeight());
			}
			return path;
		}
	}

	protected static List<RailPoint> group(double xA, double yA, String dirA, boolean elevatedA, double xB, double yB,
			String dirB, boolean elevatedB) {
		return ImmutableList.of(new RailPoint(xA, yA, dirA, elevatedA), new RailPoint(xB, yB, dirB, elevatedB));
	}

	protected static List<RailPoint> group(double xA, double yA, String dirA, boolean elevatedA, double xB, double yB,
			String dirB, boolean elevatedB, double xC, double yC, String dirC, boolean elevatedC) {
		return ImmutableList.of(new RailPoint(xA, yA, dirA, elevatedA), new RailPoint(xB, yB, dirB, elevatedB),
				new RailPoint(xC, yC, dirC, elevatedC));
	}

	protected final boolean elevated;
	private final Dir16[] validDirections;

	protected FPRailPictureSet protoPictures;
	protected Optional<FPRailFenceGraphicsSet> protoFencePictures;
	protected EnumMap<Dir16, FPVector> protoFrontEndingShifts;
	protected EnumMap<Dir16, FPVector> protoBackEndingShifts;

	private final Layer layerRailStoneBackground;
	private final Layer layerRailStone;
	private final Layer layerRailTies;
	private final Layer layerRailBackplates;
	private final Layer layerRailMetals;


	public RailRendering(boolean elevated, Dir16... validDirections) {
		this.elevated = elevated;
		this.validDirections = validDirections;

		if (elevated) {
			layerRailStoneBackground = Layer.ELEVATED_RAIL_STONE_PATH_LOWER;
			layerRailStone = Layer.ELEVATED_RAIL_STONE_PATH;
			layerRailTies = Layer.ELEVATED_RAIL_TIE;
			layerRailBackplates = Layer.ELEVATED_RAIL_SCREW;
			layerRailMetals = Layer.ELEVATED_RAIL_METAL;
		} else {
			layerRailStoneBackground = Layer.RAIL_STONE_PATH_LOWER;
			layerRailStone = Layer.RAIL_STONE_PATH;
			layerRailTies = Layer.RAIL_TIE;
			layerRailBackplates = Layer.RAIL_SCREW;
			layerRailMetals = Layer.RAIL_METAL;
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSRailEntity.class;
	}

	@Override
	protected MapRect3D computeBounds() {
		return MapRect3D.byUnit(0, 0, 0, 0, 0);// Computed by rail def
	}

	@Override
	public MapRect3D getDrawBounds(MapEntity entity) {
		RailDef railDef = getRailDef(entity);
		return railDef.drawBounds.shift(entity.getPosition());
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		FPRailPieceLayers railPieceLayers = protoPictures.get(entity.getDirection());
		if (railPieceLayers.stonePathBackground.isPresent()) {
			railPieceLayers.stonePathBackground.get()
					.defineSprites(entity.spriteRegister(register, layerRailStoneBackground), VARIATION);
		}
		if (railPieceLayers.stonePath.isPresent()) {
			railPieceLayers.stonePath.get().defineSprites(entity.spriteRegister(register, layerRailStone), VARIATION);
		}
		if (railPieceLayers.ties.isPresent()) {
			railPieceLayers.ties.get().defineSprites(entity.spriteRegister(register, layerRailTies), VARIATION);
		}
		if (railPieceLayers.backplates.isPresent()) {
			railPieceLayers.backplates.get().defineSprites(entity.spriteRegister(register, layerRailBackplates),
					VARIATION);
		}
		if (railPieceLayers.metals.isPresent()) {
			railPieceLayers.metals.get().defineSprites(entity.spriteRegister(register, layerRailMetals), VARIATION);
		}

		MapRail rail = entity.<BSRailEntity>fromBlueprint().getRail();
		if (!map.isRailConnected(rail, rail.getDef().A) && protoPictures.frontRailEndings.isPresent()) {
			FPVector endingShift = protoFrontEndingShifts.get(rail.getDef().A.dir);
			MapPosition endingPos = rail.getPos().add(rail.getDef().A.pos).addUnit(endingShift);
			protoPictures.frontRailEndings.get().defineSprites(s -> {
				register.accept(new MapSprite(s, layerRailBackplates, endingPos));
			}, rail.getDef().A.dir);
		}
		if (!map.isRailConnected(rail, rail.getDef().B) && protoPictures.backRailEndings.isPresent()) {
			FPVector endingShift = protoBackEndingShifts.get(rail.getDef().B.dir);
			MapPosition endingPos = rail.getPos().add(rail.getDef().B.pos).addUnit(endingShift);
			protoPictures.backRailEndings.get().defineSprites(s -> {
				register.accept(new MapSprite(s, layerRailBackplates, endingPos));
			}, rail.getDef().B.dir);
		}
	}

	protected abstract RailDef getRailDef(MapEntity entity);

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPictures.getDefs(register, VARIATION);
		protoFencePictures.ifPresent(fp -> fp.getDefs(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoPictures = new FPRailPictureSet(prototype.lua().get("pictures"));
		protoFencePictures = FPUtils.opt(prototype.lua().get("fence_pictures"), FPRailFenceGraphicsSet::new);

		protoFrontEndingShifts = new EnumMap<>(Dir16.class);
		protoBackEndingShifts = new EnumMap<>(Dir16.class);
		for (Dir16 dir : Dir16.values()) {
			protoFrontEndingShifts.put(dir, new FPVector(0,0));
			protoBackEndingShifts.put(dir, new FPVector(0,0));
		}

		Optional<List<FPVector>> optEndingShifts = FPUtils.optList(prototype.lua().get("ending_shifts"), FPVector::new);
		
		if (optEndingShifts.isPresent()) {
			List<FPVector> endingShifts = optEndingShifts.get();
			if (validDirections.length * 2 != endingShifts.size()) {
				throw new IllegalStateException("Invalid number of ending shifts: " + endingShifts.size() + " (expected "
						+ (validDirections.length * 2) + ")");
			}
		
			int i = 0;
			for (Dir16 dir : validDirections) {
				protoFrontEndingShifts.put(dir, endingShifts.get(i++));
				protoBackEndingShifts.put(dir, endingShifts.get(i++));
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		MapRail rail = new MapRail(entity.getPosition(), getRailDef(entity));
		map.setRail(rail);
		entity.<BSRailEntity>fromBlueprint().setRail(rail);
	}
}
