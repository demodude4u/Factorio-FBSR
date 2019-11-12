package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class LoaderRendering extends EntityRendererFactory {

	private static Point2D.Double getBeltShift(BlueprintEntity entity, EntityPrototype prototype, double offset) {
		boolean input = entity.json().getString("type").equals("input");
		Direction oppositeStructDir = input ? entity.getDirection().back() : entity.getDirection();
		double beltDistance = !prototype.lua().get("belt_distance").isnil()
				? prototype.lua().get("belt_distance").todouble()
				: 0.5;
		beltDistance += offset;
		return new Point2D.Double(beltDistance * oppositeStructDir.getDx(), beltDistance * oppositeStructDir.getDy());
	}

	private static Point2D.Double getContainerShift(BlueprintEntity entity, EntityPrototype prototype, double offset) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		double containerDistance = !prototype.lua().get("container_distance").isnil()
				? prototype.lua().get("container_distance").todouble()
				: 1.5;
		containerDistance += offset;
		return new Point2D.Double(containerDistance * structDir.getDx(), containerDistance * structDir.getDy());
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		Point2D.Double beltShift = getBeltShift(entity, prototype, 0);
		Sprite beltSprite = TransportBeltRendering.getBeltSprite(prototype, entity.getDirection(), BeltBend.NONE);
		beltSprite.bounds.x += beltShift.x;
		beltSprite.bounds.y += beltShift.y;

		Sprite sprite = RenderUtils.getSpriteFromAnimation(
				prototype.lua().get("structure").get(input ? "direction_in" : "direction_out").get("sheet"));
		sprite.source.x += sprite.source.width * (structDir.cardinal());

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, beltSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, sprite, entity, prototype));

		if (entity.json().has("filters")) {
			List<String> items = new ArrayList<>();
			Utils.<JSONObject>forEach(entity.json().getJSONArray("filters"), j -> {
				items.add(j.getString("name"));
			});

			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Optional<ItemPrototype> optItem = dataTable.getItem(itemName);
				if (optItem.isPresent()) {
					Sprite spriteIcon = new Sprite();
					spriteIcon.image = FactorioData.getIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, prototype);
					register.accept(new Renderer(Layer.OVERLAY4, delegate.getBounds()) {
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
		Direction dir = entity.getDirection();
		Point2D.Double pos = entity.getPosition();
		Point2D.Double beltShift = getBeltShift(entity, prototype, 0.5);
		Point2D.Double containerShift = getContainerShift(entity, prototype, -0.5);
		boolean input = entity.json().getString("type").equals("input");

		if (input) { // problem: should not output to belt, but does
			Direction movementDir = dir.back();
			Point2D.Double inPos = new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y);
			Point2D.Double outPos = new Point2D.Double(pos.x + containerShift.x, pos.y + containerShift.y);

			setLogisticMove(map, inPos, movementDir.backLeft(), movementDir);
			setLogisticMove(map, inPos, movementDir.backRight(), movementDir);
			setLogisticAcceptFilter(map, inPos, movementDir.frontLeft(), dir); // i dont know of I need this
			setLogisticAcceptFilter(map, inPos, movementDir.frontRight(), dir);

			addLogisticWarp(map, inPos, movementDir.backLeft(), outPos, movementDir.back());
			addLogisticWarp(map, inPos, movementDir.backRight(), outPos, movementDir.back());

		} else { // problem: should not input from belt, but does
			// Problem: doesnt accept inserter input on belt tile
			Point2D.Double inPos = new Point2D.Double(pos.x + containerShift.x, pos.y + containerShift.y);
			Point2D.Double outPos = new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y);

			setLogisticMove(map, outPos, dir.backLeft(), dir); // why is this back instead of front???
			setLogisticMove(map, outPos, dir.backRight(), dir);
			// setLogisticAcceptFilter(map, outPos, dir.frontLeft(), dir.back());
			// setLogisticAcceptFilter(map, outPos, dir.frontRight(), dir.back());

			addLogisticWarp(map, inPos, dir.back(), outPos, dir.frontLeft());
			addLogisticWarp(map, inPos, dir.back(), outPos, dir.frontRight());
		}

		if (entity.json().has("filters") && !input) { // this works. TODO: blocked output? TODO: fix icons with mip maps

			Set<String> outputs = new LinkedHashSet<>();
			Utils.<JSONObject>forEach(entity.json().getJSONArray("filters"), j -> {
				outputs.add(j.getString("name"));
			});

			map.getOrCreateLogisticGridCell(
					dir.frontLeft().offset(new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y), 0.25))
					.setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(
					dir.frontRight().offset(new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y), 0.25))
					.setOutputs(Optional.of(outputs));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");
		Point2D.Double beltShift = getBeltShift(entity, prototype, 0);

		if (input) {
			map.setBelt(new Point2D.Double(entity.getPosition().x + beltShift.x, entity.getPosition().y + beltShift.y),
					entity.getDirection(), false, false);
		} else {
			map.setBelt(new Point2D.Double(entity.getPosition().x + beltShift.x, entity.getPosition().y + beltShift.y),
					entity.getDirection(), false, true);
		}
	}
}
