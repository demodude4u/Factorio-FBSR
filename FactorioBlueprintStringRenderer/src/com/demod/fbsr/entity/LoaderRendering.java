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
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;

public class LoaderRendering extends EntityRendererFactory {

	private double protoBeltDistance;
	private double protoContainerDistance;
	private SpriteDef[][] protoBeltSprites;
	private SpriteDef protoInputSprite;
	private SpriteDef protoOutputSprite;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();

		Point2D.Double beltShift = getBeltShift(entity);

		Sprite beltSprite = protoBeltSprites[entity.getDirection().cardinal()][BeltBend.NONE.ordinal()].createSprite();
		beltSprite.bounds.x += beltShift.x;
		beltSprite.bounds.y += beltShift.y;

		Sprite sprite = (input ? protoInputSprite : protoOutputSprite).createSprite();
		sprite.source.x += sprite.source.width * (structDir.cardinal());

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, beltSprite, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, sprite, entity, protoSelectionBox));

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

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, protoSelectionBox);
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

	private Point2D.Double getBeltShift(BlueprintEntity entity) {
		boolean input = entity.json().getString("type").equals("input");
		Direction oppositeStructDir = input ? entity.getDirection().back() : entity.getDirection();
		return new Point2D.Double(protoBeltDistance * oppositeStructDir.getDx(),
				protoBeltDistance * oppositeStructDir.getDy());
	}

	private Point2D.Double getContainerShift(BlueprintEntity entity, double offset) {
		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		double containerDistance = protoContainerDistance;
		containerDistance += offset;
		return new Point2D.Double(containerDistance * structDir.getDx(), containerDistance * structDir.getDy());
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBeltSprites = TransportBeltRendering.getBeltSprites(prototype);
		protoBeltDistance = prototype.getType().equals("loader-1x1") ? 0 : 0.5;
		protoContainerDistance = prototype.lua().get("container_distance").optdouble(1.5);
		protoInputSprite = RenderUtils
				.getSpriteFromAnimation(prototype.lua().get("structure").get("direction_in").get("sheet")).get();
		protoOutputSprite = RenderUtils
				.getSpriteFromAnimation(prototype.lua().get("structure").get("direction_out").get("sheet")).get();
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction dir = entity.getDirection();
		Point2D.Double pos = entity.getPosition();
		Point2D.Double beltShift = getBeltShift(entity);
		Point2D.Double containerShift = getContainerShift(entity, -0.5);
		boolean input = entity.json().getString("type").equals("input");

		if (input) {
			Point2D.Double inPos = new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y);
			Point2D.Double outPos = new Point2D.Double(pos.x + containerShift.x, pos.y + containerShift.y);

			setLogisticMoveAndAcceptFilter(map, inPos, dir.frontLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, inPos, dir.frontRight(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, inPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, inPos, dir.backRight(), dir, dir);

			if (beltShift.x != 0 || beltShift.y != 0) { // don't do this for 1x1 loaders
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontRight(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backRight(), dir, dir);
			}

			// don't sideload on the output(chest) end of the loader either
			setLogisticAcceptFilter(map, outPos, dir.back().frontLeft(), dir);
			setLogisticAcceptFilter(map, outPos, dir.back().frontRight(), dir);

			addLogisticWarp(map, outPos, dir.back().frontLeft(), outPos, dir);
			addLogisticWarp(map, outPos, dir.back().frontRight(), outPos, dir);
			map.getOrCreateLogisticGridCell(dir.back().frontLeft().offset(outPos, 0.25)).setBlockWarpFromIfMove(true);
			map.getOrCreateLogisticGridCell(dir.back().frontRight().offset(outPos, 0.25)).setBlockWarpFromIfMove(true);

		} else {
			Point2D.Double inPos = new Point2D.Double(pos.x + containerShift.x, pos.y + containerShift.y);
			Point2D.Double outPos = new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y);

			setLogisticMoveAndAcceptFilter(map, outPos, dir.frontLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, outPos, dir.frontRight(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, outPos, dir.backLeft(), dir, dir);
			setLogisticMoveAndAcceptFilter(map, outPos, dir.backRight(), dir, dir);

			if (beltShift.x != 0 || beltShift.y != 0) { // don't do this for 1x1 loaders
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.frontRight(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backLeft(), dir, dir);
				setLogisticMoveAndAcceptFilter(map, pos, dir.backRight(), dir, dir);

				// XXX really should be a filter that accepts no direction
				setLogisticMoveAndAcceptFilter(map, inPos, dir.frontLeft(), dir, dir.back());
				setLogisticMoveAndAcceptFilter(map, inPos, dir.frontRight(), dir, dir.back());
			}

			addLogisticWarp(map, inPos, dir.back(), inPos, dir.frontLeft());
			addLogisticWarp(map, inPos, dir.back(), inPos, dir.frontRight());

			map.getOrCreateLogisticGridCell(dir.frontLeft().offset(inPos, 0.25)).setBlockWarpToIfMove(true);
			map.getOrCreateLogisticGridCell(dir.frontRight().offset(inPos, 0.25)).setBlockWarpToIfMove(true);
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
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		boolean input = entity.json().getString("type").equals("input");
		Point2D.Double beltShift = getBeltShift(entity);

		if (input) {
			map.setBelt(new Point2D.Double(entity.getPosition().x + beltShift.x, entity.getPosition().y + beltShift.y),
					entity.getDirection(), false, false);
		} else {
			map.setBelt(new Point2D.Double(entity.getPosition().x + beltShift.x, entity.getPosition().y + beltShift.y),
					entity.getDirection(), false, true);
		}
	}
}
