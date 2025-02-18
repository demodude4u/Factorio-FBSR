package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.entity.SplitterRendering.BSSplitterEntity;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class SplitterRendering extends TransportBeltConnectableRendering<BSSplitterEntity> {

	public static class BSSplitterEntity extends BSEntity {
		public final Optional<String> inputPriority;
		public final Optional<String> outputPriority;
		public final Optional<BSFilter> filter;

		public BSSplitterEntity(JSONObject json) {
			super(json);

			inputPriority = BSUtils.optString(json, "input_priority");
			outputPriority = BSUtils.optString(json, "output_priority");
			filter = BSUtils.opt(json, "filter", BSFilter::new);
		}

		public BSSplitterEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			inputPriority = BSUtils.optString(legacy.json(), "input_priority");
			outputPriority = BSUtils.optString(legacy.json(), "output_priority");
			filter = BSUtils.optString(legacy.json(), "filter").map(s -> new BSFilter(s));
		}
	}

	private static final Path2D.Double markerShape = new Path2D.Double();
	static {
		markerShape.moveTo(-0.5 + 0.2, 0.5 - 0.125);
		markerShape.lineTo(0.5 - 0.2, 0.5 - 0.125);
		markerShape.lineTo(0, 0 + 0.125);
		markerShape.closePath();
	}

	private FPAnimation4Way protoStructure;
	private Optional<FPAnimation4Way> protoStructurePatch;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSSplitterEntity entity) {
		Direction dir = entity.direction;

		Point2D.Double belt1Shift = dir.left().offset(new Point2D.Double(), 0.5);
		Point2D.Double belt2Shift = dir.right().offset(new Point2D.Double(), 0.5);

		List<Sprite> belt1Sprites = createBeltSprites(dir.cardinal(), BeltBend.NONE.ordinal(),
				getAlternatingFrame(entity.position.createPoint(belt1Shift), 0));
		List<Sprite> belt2Sprites = createBeltSprites(dir.cardinal(), BeltBend.NONE.ordinal(),
				getAlternatingFrame(entity.position.createPoint(belt2Shift), 0));

		RenderUtils.shiftSprites(belt1Sprites, belt1Shift);
		RenderUtils.shiftSprites(belt2Sprites, belt2Shift);

		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, belt1Sprites, entity, drawBounds));
		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, belt2Sprites, entity, drawBounds));

		if (protoStructurePatch.isPresent() && (dir == Direction.WEST || dir == Direction.EAST)) {
			register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER,
					protoStructurePatch.get().createSprites(data, entity.direction, 0), entity, drawBounds));
		}

		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER,
				protoStructure.createSprites(data, entity.direction, 0), entity, drawBounds));

		Point2D.Double pos = entity.position.createPoint();
		Point2D.Double leftPos = dir.left().offset(pos, 0.5);
		Point2D.Double rightPos = dir.right().offset(pos, 0.5);

		if (entity.inputPriority.isPresent() && map.isAltMode()) {
			boolean right = entity.inputPriority.get().equals("right");
			Point2D.Double inputPos = dir.offset(right ? rightPos : leftPos, 0);

			register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, inputPos, true) {
				@Override
				public void render(Graphics2D g) {
					AffineTransform pat = g.getTransform();

					Color color = Color.yellow;
					Color shadow = Color.darkGray;
					double shadowShift = 0.07;

					g.setTransform(pat);
					g.translate(inputPos.x, inputPos.y);
					g.rotate(dir.back().ordinal() * Math.PI / 4.0 + Math.PI);
					g.translate(shadowShift, shadowShift);
					g.setColor(shadow);
					g.fill(markerShape);

					g.setTransform(pat);
					g.translate(inputPos.x, inputPos.y);
					g.rotate(dir.back().ordinal() * Math.PI / 4.0 + Math.PI);
					g.setColor(color);
					g.fill(markerShape);

					g.setTransform(pat);
				}
			});
		}

		if (entity.outputPriority.isPresent() && map.isAltMode()) {
			boolean right = entity.outputPriority.get().equals("right");
			Point2D.Double outputPos = dir.offset(right ? rightPos : leftPos, 0.6);

			if (entity.filter.isPresent()) {
				Point2D.Double iconPos = right ? rightPos : leftPos;
				String itemName = entity.filter.get().name;
				Sprite spriteIcon = new Sprite();
				Optional<ItemPrototype> optItem = FactorioManager.lookupItemByName(itemName);
				if (optItem.isPresent()) {
					spriteIcon.image = optItem.get().getTable().getData().getWikiIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, drawBounds);
					spriteIcon.bounds = new Rectangle2D.Double(iconPos.x - 0.3, iconPos.y - 0.3, 0.6, 0.6);
					register.accept(new Renderer(Layer.ENTITY_INFO_ICON, delegate.getBounds(), true) {
						@Override
						public void render(Graphics2D g) throws Exception {
							g.setColor(new Color(0, 0, 0, 128));
							g.fill(spriteIcon.bounds);
							delegate.render(g);
						}
					});
				}
			} else {
				register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, outputPos, true) {
					@Override
					public void render(Graphics2D g) {
						AffineTransform pat = g.getTransform();

						Color color = Color.yellow;
						Color shadow = Color.darkGray;
						double shadowShift = 0.07;

						g.setTransform(pat);
						g.translate(outputPos.x, outputPos.y);
						g.rotate(dir.back().ordinal() * Math.PI / 4.0 + Math.PI);
						g.translate(shadowShift, shadowShift);
						g.setColor(shadow);
						g.fill(markerShape);

						g.setTransform(pat);
						g.translate(outputPos.x, outputPos.y);
						g.rotate(dir.back().ordinal() * Math.PI / 4.0 + Math.PI);
						g.setColor(color);
						g.fill(markerShape);

						g.setTransform(pat);
					}
				});
			}
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoStructurePatch = FPUtils.opt(prototype.lua().get("structure_patch"), FPAnimation4Way::new);
		protoStructure = new FPAnimation4Way(prototype.lua().get("structure"));
	}

	@Override
	public void populateLogistics(WorldMap map, BSSplitterEntity entity) {
		Direction dir = entity.direction;
		Double pos = entity.position.createPoint();
		Point2D.Double leftPos = dir.left().offset(pos, 0.5);
		Point2D.Double rightPos = dir.right().offset(pos, 0.5);

		setLogisticMoveAndAcceptFilter(map, leftPos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, leftPos, dir.frontRight(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, rightPos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, rightPos, dir.frontRight(), dir, dir);

		if (entity.outputPriority.isPresent() && entity.filter.isPresent()) {
			boolean right = entity.outputPriority.get().equals("right");
			Point2D.Double outPos = right ? rightPos : leftPos;
			Point2D.Double notOutPos = !right ? rightPos : leftPos;
			String itemName = entity.filter.get().name;

			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(outPos, 0.25)).addOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(outPos, 0.25)).addOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(notOutPos, 0.25)).addBannedOutput(itemName);
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(notOutPos, 0.25)).addBannedOutput(itemName);

			setLogisticMoveAndAcceptFilter(map, notOutPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, notOutPos, dir.backRight(), dir, dir);

			addLogisticWarp(map, outPos, dir.backLeft(), notOutPos, dir.frontLeft());
			addLogisticWarp(map, outPos, dir.backRight(), notOutPos, dir.frontRight());

			// no sideloading
			setLogisticAcceptFilter(map, outPos, dir.backLeft(), dir);
			setLogisticAcceptFilter(map, outPos, dir.backRight(), dir);

		} else {
			setLogisticMoveAndAcceptFilter(map, leftPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, leftPos, dir.backRight(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, rightPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, rightPos, dir.backRight(), dir, dir);

			addLogisticWarp(map, leftPos, dir.backLeft(), rightPos, dir.frontLeft());
			addLogisticWarp(map, leftPos, dir.backRight(), rightPos, dir.frontRight());
			addLogisticWarp(map, rightPos, dir.backLeft(), leftPos, dir.frontLeft());
			addLogisticWarp(map, rightPos, dir.backRight(), leftPos, dir.frontRight());
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, BSSplitterEntity entity) {
		Direction direction = entity.direction;
		Point2D.Double pos = entity.position.createPoint();
		Point2D.Double belt1Pos = direction.left().offset(pos, 0.5);
		Point2D.Double belt2Pos = direction.right().offset(pos, 0.5);
		map.setBelt(belt1Pos, direction, false, true);
		map.setBelt(belt2Pos, direction, false, true);
	}
}
