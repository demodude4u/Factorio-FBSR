package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.RenderUtils.SpriteDirDefList;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class SplitterRendering extends EntityRendererFactory {

	private static final Path2D.Double markerShape = new Path2D.Double();
	static {
		markerShape.moveTo(-0.5 + 0.2, 0.5 - 0.125);
		markerShape.lineTo(0.5 - 0.2, 0.5 - 0.125);
		markerShape.lineTo(0, 0 + 0.125);
		markerShape.closePath();
	}

	private SpriteDef[][] protoBeltSprites;
	private SpriteDirDefList protoPatch;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		Direction dir = entity.getDirection();

		Sprite belt1Sprite = protoBeltSprites[dir.cardinal()][BeltBend.NONE.ordinal()].createSprite();
		Sprite belt2Sprite = new Sprite(belt1Sprite);

		Point2D.Double beltShift = dir.left().offset(new Point2D.Double(), 0.5);

		belt1Sprite.bounds.x += beltShift.x;
		belt1Sprite.bounds.y += beltShift.y;
		belt2Sprite.bounds.x -= beltShift.x;
		belt2Sprite.bounds.y -= beltShift.y;

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, belt1Sprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, belt2Sprite, entity, protoSelectionBox));

		if ((protoPatch != null) && (dir == Direction.WEST || dir == Direction.EAST)) {
			register.accept(RenderUtils.spriteDirDefRenderer(Layer.ENTITY2, protoPatch, entity, protoSelectionBox));
		}

		register.accept(RenderUtils.spriteDirDefRenderer(Layer.ENTITY2, protoDirSprites, entity, protoSelectionBox));

		Double pos = entity.getPosition();
		Point2D.Double leftPos = dir.left().offset(pos, 0.5);
		Point2D.Double rightPos = dir.right().offset(pos, 0.5);

		if (entity.json().has("input_priority")) {
			boolean right = entity.json().getString("input_priority").equals("right");
			Point2D.Double inputPos = dir.offset(right ? rightPos : leftPos, 0);

			register.accept(new Renderer(Layer.OVERLAY3, inputPos) {
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

		if (entity.json().has("output_priority")) {
			boolean right = entity.json().getString("output_priority").equals("right");
			Point2D.Double outputPos = dir.offset(right ? rightPos : leftPos, 0.6);

			// TODO new format filter
			if (!entity.isJsonNewFormat() && entity.json().has("filter")) {
				Point2D.Double iconPos = right ? rightPos : leftPos;
				String itemName = entity.json().getString("filter");
				Sprite spriteIcon = new Sprite();
				Optional<ItemPrototype> optItem = dataTable.getItem(itemName);
				if (optItem.isPresent()) {
					spriteIcon.image = FactorioData.getIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, protoSelectionBox);
					spriteIcon.bounds = new Rectangle2D.Double(iconPos.x - 0.3, iconPos.y - 0.3, 0.6, 0.6);
					register.accept(new Renderer(Layer.OVERLAY2, delegate.getBounds()) {
						@Override
						public void render(Graphics2D g) throws Exception {
							g.setColor(new Color(0, 0, 0, 128));
							g.fill(spriteIcon.bounds);
							delegate.render(g);
						}
					});
				}
			} else {
				register.accept(new Renderer(Layer.OVERLAY3, outputPos) {
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
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBeltSprites = TransportBeltRendering.getBeltSprites(prototype);

		LuaValue structurePatch = prototype.lua().get("structure_patch");
		if (!structurePatch.isnil()) {
			protoPatch = RenderUtils.getDirSpritesFromAnimation(structurePatch);
		} else {
			protoPatch = null;
		}

		protoDirSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("structure"));

	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction dir = entity.getDirection();
		Double pos = entity.getPosition();
		Point2D.Double leftPos = dir.left().offset(pos, 0.5);
		Point2D.Double rightPos = dir.right().offset(pos, 0.5);

		setLogisticMoveAndAcceptFilter(map, leftPos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, leftPos, dir.frontRight(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, rightPos, dir.frontLeft(), dir, dir);
		setLogisticMoveAndAcceptFilter(map, rightPos, dir.frontRight(), dir, dir);

		// TODO new format filter
		if (!entity.isJsonNewFormat() && entity.json().has("output_priority") && entity.json().has("filter")) {
			boolean right = entity.json().getString("output_priority").equals("right");
			Point2D.Double outPos = right ? rightPos : leftPos;
			Point2D.Double notOutPos = !right ? rightPos : leftPos;
			String itemName = entity.json().getString("filter");

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
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction direction = entity.getDirection();
		Point2D.Double belt1Pos = direction.left().offset(entity.getPosition(), 0.5);
		Point2D.Double belt2Pos = direction.right().offset(entity.getPosition(), 0.5);
		map.setBelt(belt1Pos, direction, false, true);
		map.setBelt(belt2Pos, direction, false, true);
	}
}
