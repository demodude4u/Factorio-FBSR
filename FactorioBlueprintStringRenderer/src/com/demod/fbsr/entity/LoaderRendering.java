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

	private static Point2D.Double getBeltShift(BlueprintEntity entity, EntityPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");
		Direction oppositeStructDir = input ? entity.getDirection().back() : entity.getDirection();
		double beltDistance = prototype.lua().get("belt_distance").optdouble(0.5);
		return new Point2D.Double(beltDistance * oppositeStructDir.getDx(), beltDistance * oppositeStructDir.getDy());
	}

	private static Point2D.Double getContainerShift(BlueprintEntity entity, EntityPrototype prototype, double offset) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		double containerDistance = prototype.lua().get("container_distance").optdouble(1.5);
		containerDistance += offset;
		return new Point2D.Double(containerDistance * structDir.getDx(), containerDistance * structDir.getDy());
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		Point2D.Double beltShift = getBeltShift(entity, prototype);
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
		Point2D.Double beltShift = getBeltShift(entity, prototype);
		Point2D.Double containerShift = getContainerShift(entity, prototype, -0.5);
		boolean input = entity.json().getString("type").equals("input");

		if (input) {
			Point2D.Double inPos = new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y);
			Point2D.Double outPos = new Point2D.Double(pos.x + containerShift.x, pos.y + containerShift.y);

			setLogisticMove(map, inPos, dir.frontLeft(), dir);
			setLogisticMove(map, inPos, dir.frontRight(), dir);
			setLogisticMove(map, inPos, dir.backLeft(), dir);
			setLogisticMove(map, inPos, dir.backRight(), dir);

			addLogisticWarp(map, inPos, dir.back().backLeft(), outPos, dir);
			addLogisticWarp(map, inPos, dir.back().backRight(), outPos, dir);
			map.getOrCreateLogisticGridCell(dir.back().backLeft().offset(inPos, 0.25)).setBlockWarpFromIfMove(true);
			map.getOrCreateLogisticGridCell(dir.back().backRight().offset(inPos, 0.25)).setBlockWarpFromIfMove(true);

		} else {
			Point2D.Double inPos = new Point2D.Double(pos.x + containerShift.x, pos.y + containerShift.y);
			Point2D.Double outPos = new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y);

			setLogisticMove(map, outPos, dir.frontLeft(), dir);
			setLogisticMove(map, outPos, dir.frontRight(), dir);
			setLogisticMove(map, outPos, dir.backLeft(), dir);
			setLogisticMove(map, outPos, dir.backRight(), dir);

			addLogisticWarp(map, inPos, dir.back(), outPos, dir.backLeft());
			addLogisticWarp(map, inPos, dir.back(), outPos, dir.backRight());

			map.getOrCreateLogisticGridCell(dir.backLeft().offset(outPos, 0.25)).setBlockWarpToIfMove(true);
			map.getOrCreateLogisticGridCell(dir.backRight().offset(outPos, 0.25)).setBlockWarpToIfMove(true);
		}

		if (entity.json().has("filters") && !input) {

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
		Point2D.Double beltShift = getBeltShift(entity, prototype);

		if (input) {
			map.setBelt(new Point2D.Double(entity.getPosition().x + beltShift.x, entity.getPosition().y + beltShift.y),
					entity.getDirection(), false, false);
		} else {
			map.setBelt(new Point2D.Double(entity.getPosition().x + beltShift.x, entity.getPosition().y + beltShift.y),
					entity.getDirection(), false, true);
		}
	}
}
