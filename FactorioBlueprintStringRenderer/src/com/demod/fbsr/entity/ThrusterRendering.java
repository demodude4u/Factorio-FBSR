package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.fp.FPWorkingVisualisation;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

@EntityType("thruster")
public class ThrusterRendering extends EntityWithOwnerRendering {

	private static class FluidBoxWorkVis {
		private final FPFluidBox fluidBox;
		private final FPPipeConnectionDefinition pipeConnection;
		private final FPWorkingVisualisation workingVisualisation;

		public FluidBoxWorkVis(FPFluidBox fluidBox, FPPipeConnectionDefinition pipeConnection, FPWorkingVisualisation workingVisualisation) {
			this.fluidBox = fluidBox;
			this.pipeConnection = pipeConnection;
			this.workingVisualisation = workingVisualisation;
		}
	}
	private List<FluidBoxWorkVis> protoFluidCovers;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.fluidBox(lua.get("fuel_fluid_box"));
		bind.fluidBox(lua.get("oxidizer_fluid_box"));
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Direction dir = entity.getDirection();
		for (FluidBoxWorkVis fbwv : protoFluidCovers) {
			FPFluidBox fluidBox = fbwv.fluidBox;
			FPPipeConnectionDefinition conn = fbwv.pipeConnection;
			FPWorkingVisualisation workingVisualisation = fbwv.workingVisualisation;
				
			Direction connDir = conn.direction.get();
			MapPosition connPos = MapPosition.convert(conn.position.get());
			Direction facing = connDir.rotate(dir);
			MapPosition point = facing.offset(dir.rotate(connPos).add(entity.getPosition()), 1);
			if (map.isPipe(point, facing)) {
				workingVisualisation.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), Direction.NORTH, 0);
			}
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		Map<String, FPWorkingVisualisation> allWorkVisByName = new HashMap<>();
		FPWorkingVisualisations protoGraphicsSet = new FPWorkingVisualisations(profile, prototype.lua().get("graphics_set"));
		if (protoGraphicsSet.workingVisualisations.isPresent()) {
			for (FPWorkingVisualisation protoWorkingVisualisation : protoGraphicsSet.workingVisualisations.get()) {
				if (protoWorkingVisualisation.name.isPresent()) {
					allWorkVisByName.put(protoWorkingVisualisation.name.get(), protoWorkingVisualisation);
				}
			}
		}

		protoFluidCovers = new ArrayList<>();
		FPFluidBox protoFBFuel = new FPFluidBox(profile, prototype.lua().get("fuel_fluid_box"));
		FPFluidBox protoFBOxidizer = new FPFluidBox(profile, prototype.lua().get("oxidizer_fluid_box"));
		for (FPFluidBox fb : List.of(protoFBFuel, protoFBOxidizer)) {
			for (FPPipeConnectionDefinition pipeConn : fb.pipeConnections) {
				List<FPWorkingVisualisation> workVis = new ArrayList<>();
				for (String wvName : pipeConn.enableWorkingVisualisations) {
					FPWorkingVisualisation wv = allWorkVisByName.get(wvName);
					if (wv != null) {
						protoFluidCovers.add(new FluidBoxWorkVis(fb, pipeConn, wv));
					}
				}
			}
		}
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);
		for (FluidBoxWorkVis workVis : protoFluidCovers) {
			workVis.workingVisualisation.defineSprites(register, Direction.NORTH, 0);
		}
	}
}
