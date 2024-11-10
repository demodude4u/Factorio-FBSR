package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class BoilerRendering extends EntityRendererFactory {
	private boolean protoHasEnergySource;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoDirSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("structure"));
		protoHasEnergySource = !prototype.lua().get("energy_source").isnil();
	}

	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, BlueprintEntity entity) {
		return map.isPipe(direction.offset(entity.getPosition()), direction.back());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction dir = entity.getDirection();
		Point2D.Double position = dir.back().offset(entity.getPosition(), 0.5);
		map.setPipe(dir.offset(position, 1), dir);
		map.setPipe(dir.left().offset(position, 1), dir.left());
		map.setPipe(dir.right().offset(position, 1), dir.right());

		if (protoHasEnergySource) {
			map.setHeatPipe(position, dir.back());
		}
	}

}
