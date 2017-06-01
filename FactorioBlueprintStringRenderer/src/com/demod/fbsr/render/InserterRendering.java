package com.demod.fbsr.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.WorldMap.BeltCell;

public class InserterRendering extends TypeRendererFactory {

	private static final int[][] placeItemDir = //
			new int[/* Cardinal */][/* Bend */] { //
					{ -1, 1, 1 }, // North
					{ 1, -1, -1 }, // East
					{ -1, -1, 1 }, // South
					{ 1, 1, -1 },// West
			};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("platform_picture").get("sheet"));
		sprite.source.x += sprite.source.width * (dir.back().cardinal());

		Sprite spriteArm1 = getSpriteFromAnimation(prototype.lua().get("hand_base_picture"));
		Sprite spriteArm2 = getSpriteFromAnimation(prototype.lua().get("hand_open_picture"));
		double armStretch = -prototype.lua().get("pickup_position").get(2).todouble();

		register.accept(spriteRenderer(sprite, entity, prototype));
		register.accept(new Renderer(Layer.ENTITY2, sprite.bounds) {
			@Override
			public void render(Graphics2D g) {
				AffineTransform pat = g.getTransform();

				{
					Rectangle2D.Double bounds = spriteArm2.bounds;
					Rectangle source = spriteArm2.source;
					BufferedImage image = spriteArm2.image;

					g.translate(pos.x, pos.y);
					g.rotate(dir.back().ordinal() * Math.PI / 4.0);
					g.translate(bounds.x, 0);
					g.scale(bounds.width, armStretch);
					g.drawImage(image, 0, 1, 1, 0, source.x, source.y, source.x + source.width,
							source.y + source.height, null);
				}

				g.setTransform(pat);
			}
		});
		// register.accept(new Renderer(Layer.LOGISTICS_MOVE, sprite.bounds) {
		// @Override
		// public void render(Graphics2D g) {
		// double armStretch =
		// -prototype.lua().get("pickup_position").get(2).todouble();
		// Point2D.Double inPos = dir.offset(pos, armStretch);
		// Point2D.Double outPos = dir.offset(pos, -armStretch);
		//
		// map.getBelt(outPos).ifPresent(b -> {
		// BeltBend bend = map.getBeltBend(outPos, b);
		// Direction cellDir = dir.back().rotate(
		// placeItemDir[b.getFacing().rotate(-dir.back().ordinal()).cardinal()][bend.ordinal()]);
		//
		// g.setColor(Color.cyan);
		// g.draw(new Line2D.Double(pos, cellDir.offset(outPos, 0.25)));
		// g.setColor(Color.magenta);
		// g.drawString(dir.back().cardinal() + "|" + b.getFacing().cardinal() +
		// "|" + bend.ordinal(),
		// (float) outPos.x, (float) outPos.y);
		// });
		// }
		// });
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		double armStretch = -prototype.lua().get("pickup_position").get(2).todouble();
		Point2D.Double inPos = dir.offset(pos, armStretch);
		Point2D.Double outPos = dir.offset(pos, -armStretch);

		Optional<BeltCell> belt = map.getBelt(outPos);
		belt.ifPresent(b -> {
			BeltBend bend = map.getBeltBend(outPos, b);
			Direction cellDir = dir.back()
					.rotate(placeItemDir[b.getFacing().rotate(-dir.back().ordinal()).cardinal()][bend.ordinal()]);

			setLogisticWarp(map, inPos, dir.frontLeft(), outPos, cellDir);
			setLogisticWarp(map, inPos, dir.frontRight(), outPos, cellDir);
			setLogisticWarp(map, inPos, dir.backLeft(), outPos, cellDir);
			setLogisticWarp(map, inPos, dir.backRight(), outPos, cellDir);
		});
		if (!belt.isPresent()) {
			setLogisticWarp(map, inPos, dir.frontLeft(), outPos, dir.frontRight());
			setLogisticWarp(map, inPos, dir.frontRight(), outPos, dir.frontRight());
			setLogisticWarp(map, inPos, dir.backLeft(), outPos, dir.frontRight());
			setLogisticWarp(map, inPos, dir.backRight(), outPos, dir.frontRight());
		}
	}

}
