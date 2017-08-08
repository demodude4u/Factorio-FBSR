package com.demod.fbsr.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;

public class InserterRendering extends EntityRendererFactory {

	private static final int[][] placeItemDir = //
			new int[/* Cardinal */][/* Bend */] { //
					{ -1, 1, 1 }, // North
					{ 1, -1, -1 }, // East
					{ -1, -1, 1 }, // South
					{ 1, 1, -1 },// West
			};

	private static final Path2D.Double placeMarkerShape = new Path2D.Double();
	private static final Path2D.Double grabMarkerShape = new Path2D.Double();
	static {
		final double inset = 0.15;

		placeMarkerShape.moveTo(-0.5 + inset, 0.5 - inset);
		placeMarkerShape.lineTo(0.5 - inset, 0.5 - inset);
		placeMarkerShape.lineTo(0, 0 + inset);
		placeMarkerShape.closePath();

		grabMarkerShape.moveTo(-0.5 + inset, -0.5 + inset);
		grabMarkerShape.lineTo(0.5 - inset, -0.5 + inset);
		grabMarkerShape.lineTo(0.5 - inset, -0.5 + inset + 0.15);
		grabMarkerShape.lineTo(-0.5 + inset, -0.5 + inset + 0.15);
		grabMarkerShape.closePath();
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("platform_picture").get("sheet"));
		sprite.source.x += sprite.source.width * (dir.back().cardinal());

		// Sprite spriteArmBase =
		// getSpriteFromAnimation(prototype.lua().get("hand_base_picture"));
		Sprite spriteArmHand = RenderUtils.getSpriteFromAnimation(prototype.lua().get("hand_open_picture"));
		double armStretch = -prototype.lua().get("pickup_position").get(2).todouble();
		// Point2D.Double inPos = dir.offset(pos, armStretch);
		// Point2D.Double outPos = dir.offset(pos, -armStretch);

		boolean modded = entity.json().has("pickup_position") || entity.json().has("drop_position");

		Point2D.Double pickupPos;
		Point2D.Double insertPos;
		Point2D.Double inPos;
		Point2D.Double outPos;

		if (entity.json().has("pickup_position")) {
			pickupPos = Utils.parsePoint2D(entity.json().getJSONObject("pickup_position"));
			modded = true;
			inPos = new Point2D.Double(pos.x + pickupPos.x, pos.y + pickupPos.y);

		} else if (modded) {
			inPos = dir.offset(pos, armStretch);
			pickupPos = new Point2D.Double(inPos.x - pos.x, inPos.y - pos.y);

		} else {
			pickupPos = Utils.parsePoint2D(prototype.lua().get("pickup_position"));
			inPos = dir.offset(pos, armStretch);
		}

		if (entity.json().has("drop_position")) {
			insertPos = Utils.parsePoint2D(entity.json().getJSONObject("drop_position"));
			modded = true;
			outPos = new Point2D.Double(pos.x + insertPos.x, pos.y + insertPos.y);

		} else if (modded) {
			outPos = dir.offset(pos, -armStretch);
			insertPos = new Point2D.Double(outPos.x - pos.x, outPos.y - pos.y);

		} else {
			insertPos = Utils.parsePoint2D(prototype.lua().get("insert_position"));
			outPos = dir.offset(pos, -armStretch);
		}

		// XXX Thanks Java...
		final Point2D.Double pickupPos_ = pickupPos;
		final Point2D.Double insertPos_ = insertPos;
		final boolean modded_ = modded;

		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
		register.accept(new Renderer(Layer.ENTITY2, sprite.bounds) {
			@Override
			public void render(Graphics2D g) {
				AffineTransform pat = g.getTransform();

				Rectangle2D.Double bounds = spriteArmHand.bounds;
				Rectangle source = spriteArmHand.source;
				BufferedImage image = spriteArmHand.image;

				g.translate(pos.x, pos.y);
				g.rotate(dir.back().ordinal() * Math.PI / 4.0);
				g.translate(bounds.x, 0);
				g.scale(bounds.width, armStretch);
				g.drawImage(image, 0, 1, 1, 0, source.x, source.y, source.x + source.width, source.y + source.height,
						null);

				g.setTransform(pat);
			}
		});
		register.accept(new Renderer(Layer.OVERLAY3, inPos) {
			@Override
			public void render(Graphics2D g) throws Exception {
				AffineTransform pat = g.getTransform();

				Color color = Color.yellow;
				Color shadow = Color.darkGray;
				double shadowShift = 0.07;

				double pickupRotate = Math.atan2(pickupPos_.y, pickupPos_.x);

				g.setTransform(pat);
				g.translate(inPos.x, inPos.y);
				if (modded_) {
					g.translate(-Math.cos(pickupRotate) * 0.2, -Math.sin(pickupRotate) * 0.2);
					g.rotate(pickupRotate + Math.PI / 2.0);
				} else {
					g.rotate(dir.back().ordinal() * Math.PI / 4.0);
				}
				g.translate(shadowShift, shadowShift);
				g.setColor(shadow);
				g.fill(grabMarkerShape);

				if (modded_) {
					g.setTransform(pat);
					// RenderUtils.createWireRenderer(pos, inPos,
					// RenderUtils.withAlpha(color, 128)).render(g);
					g.setColor(RenderUtils.withAlpha(color, 64));
					g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.draw(new Line2D.Double(pos, inPos));
				}

				g.setTransform(pat);
				g.translate(inPos.x, inPos.y);
				if (modded_) {
					g.translate(-Math.cos(pickupRotate) * 0.2, -Math.sin(pickupRotate) * 0.2);
					g.rotate(pickupRotate + Math.PI / 2.0);
				} else {
					g.rotate(dir.back().ordinal() * Math.PI / 4.0);
				}
				g.setColor(color);
				g.fill(grabMarkerShape);

				g.setTransform(pat);
			}
		});
		register.accept(new Renderer(Layer.OVERLAY3, outPos) {
			@Override
			public void render(Graphics2D g) throws Exception {
				AffineTransform pat = g.getTransform();

				Color color = Color.yellow;
				Color shadow = Color.darkGray;
				double shadowShift = 0.07;

				double insertRotate = Math.atan2(insertPos_.y, insertPos_.x);

				g.setTransform(pat);
				g.translate(outPos.x, outPos.y);
				if (modded_) {
					g.translate(Math.cos(insertRotate) * 0.2, Math.sin(insertRotate) * 0.2);
					g.rotate(insertRotate + Math.PI / 2.0);
				} else {
					g.rotate(dir.back().ordinal() * Math.PI / 4.0);
				}
				g.translate(shadowShift, shadowShift);
				g.setColor(shadow);
				g.fill(placeMarkerShape);

				if (modded_) {
					g.setTransform(pat);
					// RenderUtils.createWireRenderer(pos, outPos,
					// RenderUtils.withAlpha(color, 128)).render(g);
					g.setColor(RenderUtils.withAlpha(color, 64));
					g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.draw(new Line2D.Double(pos, outPos));
				}

				g.setTransform(pat);
				g.translate(outPos.x, outPos.y);
				if (modded_) {
					g.translate(Math.cos(insertRotate) * 0.2, Math.sin(insertRotate) * 0.2);
					g.rotate(insertRotate + Math.PI / 2.0);
				} else {
					g.rotate(dir.back().ordinal() * Math.PI / 4.0);
				}
				g.setColor(color);
				g.fill(placeMarkerShape);

				g.setTransform(pat);
			}
		});

		if (entity.json().has("filters")) {
			List<String> items = new ArrayList<>();
			Utils.forEach(entity.json().getJSONArray("filters"), (JSONObject j) -> {
				items.add(j.getString("name"));
			});

			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Sprite spriteIcon = new Sprite();
				Optional<ItemPrototype> optItem = dataTable.getItem(itemName);
				if (optItem.isPresent()) {
					spriteIcon.image = FactorioData.getIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, prototype);
					register.accept(new Renderer(Layer.OVERLAY2, delegate.getBounds()) {
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
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		if (entity.json().has("pickup_position") || entity.json().has("drop_position")) {
			return; // TODO Modded inserter logistics
		}

		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		double armStretch = -prototype.lua().get("pickup_position").get(2).todouble();
		Point2D.Double inPos = dir.offset(pos, armStretch);
		Point2D.Double outPos = dir.offset(pos, -armStretch);

		Direction cellDir;

		Optional<BeltCell> belt = map.getBelt(outPos);
		if (belt.isPresent()) {
			BeltBend bend = map.getBeltBend(outPos, belt.get());
			cellDir = dir.back().rotate(
					placeItemDir[belt.get().getFacing().rotate(-dir.back().ordinal()).cardinal()][bend.ordinal()]);
		} else {
			cellDir = dir.frontRight();
		}

		if (entity.json().has("filters")) {
			LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(outPos, 0.25));
			Utils.forEach(entity.json().getJSONArray("filters"), (JSONObject j) -> {
				cell.addOutput(j.getString("name"));
			});

		} else {
			addLogisticWarp(map, inPos, dir.frontLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.frontRight(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backLeft(), outPos, cellDir);
			addLogisticWarp(map, inPos, dir.backRight(), outPos, cellDir);
		}
	}

}
