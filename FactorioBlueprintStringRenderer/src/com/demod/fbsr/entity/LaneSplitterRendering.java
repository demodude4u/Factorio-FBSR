package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Renderer;

import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSSplitterEntity;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapLaneArrow;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public class LaneSplitterRendering extends TransportBeltConnectableRendering {
	private static final int STRUCTURE_FRAME = 0;
	private static final int STRUCTURE_PATCH_FRAME = 0;

	private FPAnimation4Way protoStructure;
	private Optional<FPAnimation4Way> protoStructurePatch;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		BSSplitterEntity bsEntity = entity.<BSSplitterEntity>fromBlueprint();

		MapPosition beltPos = entity.getPosition();
		Consumer<SpriteDef> beltRegister = s -> register.accept(new MapSprite(s, Layer.TRANSPORT_BELT, beltPos));
		defineBeltSprites(beltRegister, dir.cardinal(), BeltBend.NONE.ordinal(), getAlternatingFrame(beltPos));

		if (protoStructurePatch.isPresent() && (dir == Direction.WEST || dir == Direction.EAST)) {
			protoStructurePatch.get().defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), dir,
					STRUCTURE_PATCH_FRAME);
		}

		protoStructure.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), dir, STRUCTURE_FRAME);

		MapPosition pos = entity.getPosition();
		MapPosition leftPos = dir.left().offset(pos, 0.25);
		MapPosition rightPos = dir.right().offset(pos, 0.25);

		if (bsEntity.inputPriority.isPresent() && map.isAltMode()) {
			boolean right = bsEntity.inputPriority.get().equals("right");
			MapPosition inputPos = dir.offset(right ? rightPos : leftPos, 0);

			register.accept(new MapLaneArrow(inputPos, dir));
		}

		if (bsEntity.outputPriority.isPresent() && map.isAltMode()) {
			boolean right = bsEntity.outputPriority.get().equals("right");
			MapPosition outputPos = dir.offset(right ? rightPos : leftPos, 0.6);

			if (bsEntity.filter.isPresent()) {
				MapPosition iconPos = right ? rightPos : leftPos;
				String itemName = bsEntity.filter.get().name;
				Optional<BufferedImage> icon = TagManager.lookup("item", itemName);
				if (icon.isPresent()) {
					register.accept(new MapIcon(iconPos, icon.get(), 0.6, 0.1, false));
				}

			} else {
				register.accept(new MapLaneArrow(outputPos, dir));
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
	public void populateLogistics(WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		MapPosition pos = entity.getPosition();
		MapPosition leftPos = dir.left().offset(pos, 0.5);
		MapPosition rightPos = dir.right().offset(pos, 0.5);

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

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSSplitterEntity.class;
	}
}
