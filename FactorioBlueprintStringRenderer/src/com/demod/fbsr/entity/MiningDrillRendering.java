package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPWorkingVisualisations;

public class MiningDrillRendering extends SimpleEntityRendering<BSEntity> {

	private Optional<FPSprite4Way> protoBasePicture;
	private Optional<FPWorkingVisualisations> protoGraphicsSet;
	private List<Point2D.Double> protoFluidBoxOffsets;

	// pumpjack:
	// - base_picture
	// - graphics_set.animation

	// electric-mining-drill:
	// - graphics_set.animation
	// - graphics_set.working_visualisations.N/S/E/W

	// burner-mining-drill:
	// - graphics_set.animation

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		if (protoBasePicture.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(protoBasePicture.get().createSprites(entity.direction), entity,
					protoSelectionBox));
		}

		if (protoGraphicsSet.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(protoGraphicsSet.get().createSprites(entity.direction, 0),
					entity, protoSelectionBox));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBasePicture = FPUtils.opt(prototype.lua().get("base_picture"), FPSprite4Way::new);
		protoGraphicsSet = FPUtils.opt(prototype.lua().get("graphics_set"), FPWorkingVisualisations::new);

		protoFluidBoxOffsets = new ArrayList<>();
		LuaValue luaOutputFluidBox = prototype.lua().get("output_fluid_box");
		if (!luaOutputFluidBox.isnil()) {
			Utils.forEach(luaOutputFluidBox.get("pipe_connections").get(1).get("positions"), l -> {
				protoFluidBoxOffsets.add(Utils.parsePoint2D(l));
			});
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		// TODO hardcoded
		if (entity.name.equals("pumpjack")) {
			Point2D.Double entityPos = entity.position.createPoint();
			Point2D.Double pipePos = entity.direction.back()
					.offset(protoFluidBoxOffsets.get(entity.direction.cardinal()));
			pipePos.x += entityPos.x;
			pipePos.y += entityPos.y;

			map.setPipe(pipePos, entity.direction);
		}
	}
}
