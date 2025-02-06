package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.entity.LoaderRendering.BSLoaderEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public abstract class LoaderRendering extends TransportBeltConnectableRendering<BSLoaderEntity> {

	public static class BSLoaderEntity extends BSEntity {
		public final Optional<String> type;
		public final List<BSFilter> filters;

		public BSLoaderEntity(JSONObject json) {
			super(json);

			type = BSUtils.optString(json, "type");
			filters = BSUtils.list(json, "filters", BSFilter::new);
		}

		public BSLoaderEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			type = BSUtils.optString(legacy.json(), "type");
			filters = BSUtils.list(legacy.json(), "filters", j -> new BSFilter(j.getString("name")));
		}

	}

	private final double beltDistance;

	private double protoContainerDistance;
	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;

	public LoaderRendering(double beltDistance) {
		this.beltDistance = beltDistance;
	}

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSLoaderEntity entity) {
		Point2D.Double beltShift = getBeltShift(entity);
		List<Sprite> beltSprites = createBeltSprites(entity.direction.cardinal(), BeltBend.NONE.ordinal(),
				getAlternatingFrame(entity.position.createPoint(beltShift), 0));
		RenderUtils.shiftSprites(beltSprites, beltShift);
		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, beltSprites, entity, protoSelectionBox));

		boolean input = entity.type.get().equals("input");
		Direction structDir = input ? entity.direction : entity.direction.back();
		List<Sprite> structureSprites = (input ? protoStructureDirectionIn : protoStructureDirectionOut)
				.createSprites(structDir);
		register.accept(
				RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, structureSprites, entity, protoSelectionBox));

		if (!entity.filters.isEmpty() && map.isAltMode()) {
			List<String> items = entity.filters.stream().map(bs -> bs.name).collect(Collectors.toList());

			// TODO double/quad icons
			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Optional<ItemPrototype> optItem = dataTable.getItem(itemName);
				if (optItem.isPresent()) {
					Sprite spriteIcon = new Sprite();
					spriteIcon.image = FactorioData.getIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, protoSelectionBox);
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

	private Point2D.Double getBeltShift(BSLoaderEntity entity) {
		boolean input = entity.type.get().equals("input");
		Direction oppositeStructDir = input ? entity.direction.back() : entity.direction;
		return new Point2D.Double(beltDistance * oppositeStructDir.getDx(), beltDistance * oppositeStructDir.getDy());
	}

	private Point2D.Double getContainerShift(BSLoaderEntity entity, double offset) {
		boolean input = entity.type.get().equals("input");
		Direction structDir = input ? entity.direction : entity.direction.back();
		double containerDistance = protoContainerDistance;
		containerDistance += offset;
		return new Point2D.Double(containerDistance * structDir.getDx(), containerDistance * structDir.getDy());
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoContainerDistance = prototype.lua().get("container_distance").optdouble(1.5);
		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(luaStructure.get("direction_out"));
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSLoaderEntity entity) {
		Direction dir = entity.direction;
		Point2D.Double pos = entity.position.createPoint();
		Point2D.Double beltShift = getBeltShift(entity);
		Point2D.Double containerShift = getContainerShift(entity, -0.5);
		boolean input = entity.type.get().equals("input");

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

		// TODO new format filters
		if (!entity.filters.isEmpty() && !input) {

			Set<String> outputs = entity.filters.stream().map(bs -> bs.name)
					.collect(Collectors.toCollection(LinkedHashSet::new));

			map.getOrCreateLogisticGridCell(
					dir.frontLeft().offset(new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y), 0.25))
					.setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(
					dir.frontRight().offset(new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y), 0.25))
					.setOutputs(Optional.of(outputs));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSLoaderEntity entity) {
		boolean input = entity.type.get().equals("input");
		Point2D.Double beltShift = getBeltShift(entity);

		Point2D.Double pos = entity.position.createPoint();
		if (input) {
			map.setBelt(new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y), entity.direction, false, false);
		} else {
			map.setBelt(new Point2D.Double(pos.x + beltShift.x, pos.y + beltShift.y), entity.direction, false, true);
		}
	}
}
