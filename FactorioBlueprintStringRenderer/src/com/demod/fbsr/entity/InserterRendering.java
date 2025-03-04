package com.demod.fbsr.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.Renderer;

import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.entity.InserterRendering.BSInserterEntity;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class InserterRendering extends SimpleEntityRendering<BSInserterEntity> {

	public static class BSInserterEntity extends BSEntity {
		public final Optional<BSPosition> pickupPosition;
		public final Optional<BSPosition> dropPosition;
		public final boolean useFilters;
		public final List<BSFilter> filters;

		public BSInserterEntity(JSONObject json) {
			super(json);

			useFilters = json.optBoolean("use_filters");// only sometimes checked
			pickupPosition = BSUtils.optPosition(json, "pickup_position");
			dropPosition = BSUtils.optPosition(json, "drop_position");
			filters = BSUtils.list(json, "filters", BSFilter::new);
		}

		public BSInserterEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			pickupPosition = BSUtils.opt(legacy.json(), "pickup_position", j -> {
				Point2D.Double pos = Utils.parsePoint2D(j);
				return new BSPosition(pos.x, pos.y);
			});
			dropPosition = BSUtils.opt(legacy.json(), "drop_position", j -> {
				Point2D.Double pos = Utils.parsePoint2D(j);
				return new BSPosition(pos.x, pos.y);
			});

			filters = BSUtils.list(legacy.json(), "filters", j -> new BSFilter(j.getString("name")));
			useFilters = filters.size() > 0;
		}
	}

	private static final int[][] placeItemDir = //
			new int[/* Cardinal */][/* Bend */] { //
					{ -1, 1, 1 }, // North
					{ 1, -1, -1 }, // East
					{ -1, -1, 1 }, // South
					{ 1, 1, -1 },// West
			};

	private FPSprite4Way protoPlatformPicture;
	private FPSprite protoHandOpenPicture;
	private FPVector protoPickupPosition;
	private FPVector protoInsertPosition;
	private FPSprite protoIndicationLine;
	private FPSprite protoIndicationArrow;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSInserterEntity entity) {
		super.createRenderers(register, map, entity);

		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		List<Sprite> platformSprites = protoPlatformPicture.createSprites(data, dir.back());

		boolean modded = entity.pickupPosition.isPresent() || entity.dropPosition.isPresent();

		Point2D.Double pickupPos;
		Point2D.Double insertPos;
		Point2D.Double inPos;
		Point2D.Double outPos;

		double armStretch = protoPickupPosition.y;

		if (entity.pickupPosition.isPresent()) {
			pickupPos = entity.pickupPosition.get().createPoint();
			inPos = new Point2D.Double(pos.x + pickupPos.x, pos.y + pickupPos.y);

		} else if (modded) {
			inPos = dir.offset(pos, -armStretch);
			pickupPos = new Point2D.Double(inPos.x - pos.x, inPos.y - pos.y);

		} else {
			pickupPos = protoPickupPosition.createPoint();
			inPos = dir.offset(pos, -armStretch);
		}

		if (entity.dropPosition.isPresent()) {
			insertPos = entity.dropPosition.get().createPoint();
			outPos = new Point2D.Double(pos.x + insertPos.x, pos.y + insertPos.y);

		} else if (modded) {
			outPos = dir.offset(pos, armStretch);
			insertPos = new Point2D.Double(outPos.x - pos.x, outPos.y - pos.y);

		} else {
			insertPos = protoInsertPosition.createPoint();
			outPos = dir.offset(pos, armStretch);
		}

		register.accept(RenderUtils.spriteRenderer(platformSprites, entity, drawBounds));
		register.accept(new Renderer(Layer.HIGHER_OBJECT_UNDER, pos, true) {
			@Override
			public void render(Graphics2D g) {
				AffineTransform pat = g.getTransform();

				for (Sprite sprite : protoHandOpenPicture.defineSprites(data)) {
					Rectangle2D.Double bounds = sprite.bounds;
					Rectangle source = sprite.source;
					BufferedImage image = sprite.image;

					g.translate(pos.x, pos.y);
					g.rotate(dir.ordinal() * Math.PI / 4.0);
					g.translate(bounds.x, 0);
					g.scale(bounds.width, armStretch);
					g.drawImage(image, 0, 1, 1, 0, source.x, source.y, source.x + source.width,
							source.y + source.height, null);

					g.setTransform(pat);
				}
			}
		});
		if (map.isAltMode()) {
			register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, pos, true) {
				@Override
				public void render(Graphics2D g) {
					AffineTransform pat = g.getTransform();

					double pickupRotate = Math.atan2(pickupPos.y, pickupPos.x);

					for (Sprite sprite : protoIndicationLine.defineSprites(data)) {
						Rectangle2D.Double bounds = sprite.bounds;
						Rectangle source = sprite.source;
						BufferedImage image = sprite.image;

						if (modded) {
							g.setTransform(pat);
							g.setColor(RenderUtils.withAlpha(Color.yellow, 64));
							g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
							g.draw(new Line2D.Double(pos, inPos));
						}

						g.setTransform(pat);
						g.translate(inPos.x, inPos.y);
						// HACK magic numbers
						Point2D.Double magicImageShift = new Point2D.Double(bounds.x + 0.1, bounds.y + -0.05);
						g.translate(magicImageShift.x, magicImageShift.y);
						if (modded) {
							g.translate(-Math.cos(pickupRotate) * 0.2, -Math.sin(pickupRotate) * 0.2);
							g.rotate(pickupRotate + Math.PI / 2.0, -magicImageShift.x, -magicImageShift.y);
						} else {
							g.rotate(dir.back().ordinal() * Math.PI / 4.0, -magicImageShift.x, -magicImageShift.y);
						}
						// magic numbers from Factorio code
						g.scale(0.8, 0.8);
						g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width,
								source.y + source.height, null);

						g.setTransform(pat);
					}
				}
			});
			register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, pos, true) {
				@Override
				public void render(Graphics2D g) {
					AffineTransform pat = g.getTransform();

					double insertRotate = Math.atan2(insertPos.y, insertPos.x);

					for (Sprite sprite : protoIndicationArrow.defineSprites(data)) {
						Rectangle2D.Double bounds = sprite.bounds;
						Rectangle source = sprite.source;
						BufferedImage image = sprite.image;

						if (modded) {
							g.setTransform(pat);
							g.setColor(RenderUtils.withAlpha(Color.yellow, 64));
							g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
							g.draw(new Line2D.Double(pos, outPos));
						}

						g.setTransform(pat);
						g.translate(outPos.x, outPos.y);
						// HACK magic numbers
						Point2D.Double magicImageShift = new Point2D.Double(bounds.x + 0.1, bounds.y + 0.35);
						g.translate(magicImageShift.x, magicImageShift.y);
						if (modded) {
							g.translate(Math.cos(insertRotate) * 0.2, Math.sin(insertRotate) * 0.2);
							g.rotate(insertRotate + Math.PI / 2.0, -magicImageShift.x, -magicImageShift.y);
						} else {
							g.rotate(dir.back().ordinal() * Math.PI / 4.0, -magicImageShift.x, -magicImageShift.y);
						}
						// magic numbers from Factorio code
						g.scale(0.8, 0.8);
						g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width,
								source.y + source.height, null);

						g.setTransform(pat);
					}
				}
			});
		}

		if (entity.useFilters && map.isAltMode()) {
			List<String> items = entity.filters.stream().map(bs -> bs.name).collect(Collectors.toList());

			// TODO show double/quad icons if more than one
			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Sprite spriteIcon = new Sprite();
				Optional<ItemPrototype> optItem = FactorioManager.lookupItemByName(itemName);
				if (optItem.isPresent()) {
					spriteIcon.image = optItem.get().getTable().getData().getWikiIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, drawBounds);
					register.accept(new Renderer(Layer.ENTITY_INFO_ICON, delegate.getBounds(), true) {
						@Override
						public void render(Graphics2D g) throws Exception {
							g.setColor(new Color(0, 0, 0, 128));
							g.fill(spriteIcon.bounds);
							delegate.render(g);
						}
					});
				}
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoPlatformPicture = new FPSprite4Way(prototype.lua().get("platform_picture"));
		protoHandOpenPicture = new FPSprite(prototype.lua().get("hand_open_picture"));

		protoPickupPosition = new FPVector(prototype.lua().get("pickup_position"));
		protoInsertPosition = new FPVector(prototype.lua().get("insert_position"));

		LuaValue optUtilityConstantsLua = data.getTable().getRaw("utility-sprites", "default").get();
		protoIndicationLine = new FPSprite(optUtilityConstantsLua.get("indication_line"));
		protoIndicationArrow = new FPSprite(optUtilityConstantsLua.get("indication_arrow"));
	}

	@Override
	public void populateLogistics(WorldMap map, BSInserterEntity entity) {
		if (entity.pickupPosition.isPresent() || entity.dropPosition.isPresent()) {
			return; // TODO Modded inserter logistics
		}

		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		Point2D.Double inPos = dir.offset(pos, -protoPickupPosition.y);
		Point2D.Double outPos = dir.offset(pos, protoPickupPosition.y);

		Direction cellDir;

		Optional<BeltCell> belt = map.getBelt(outPos);
		if (belt.isPresent()) {
			BeltBend bend = map.getBeltBend(outPos, belt.get());
			cellDir = dir.back().rotate(
					placeItemDir[belt.get().getFacing().rotate(-dir.back().ordinal()).cardinal()][bend.ordinal()]);
		} else {
			cellDir = dir.frontRight();
		}

		if (entity.useFilters) {
			LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(outPos, 0.25));
			entity.filters.stream().forEach(bs -> cell.addOutput(bs.name));

		} else {
			addLogisticWarp(map, inPos, dir.frontLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.frontRight(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backRight(), outPos, cellDir);
		}
	}

}
