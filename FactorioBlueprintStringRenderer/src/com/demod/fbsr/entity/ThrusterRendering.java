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
import com.demod.fbsr.bind.Bindings;
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

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		
		FPFluidBox protoFBFuel = new FPFluidBox(profile, lua.get("fuel_fluid_box"));
		FPFluidBox protoFBOxidizer = new FPFluidBox(profile, lua.get("oxidizer_fluid_box"));
		bind.fluidBox(protoFBFuel);
		bind.fluidBox(protoFBOxidizer);

		Map<String, FPWorkingVisualisation> wvLookup = new HashMap<>();
		FPWorkingVisualisations protoGraphicsSet = new FPWorkingVisualisations(profile, lua.get("graphics_set"));
		if (protoGraphicsSet.workingVisualisations.isPresent()) {
			for (FPWorkingVisualisation protoWorkingVisualisation : protoGraphicsSet.workingVisualisations.get()) {
				if (protoWorkingVisualisation.name.isPresent()) {
					wvLookup.put(protoWorkingVisualisation.name.get(), protoWorkingVisualisation);
				}
			}
		}
		
		for (FPFluidBox fb : List.of(protoFBFuel, protoFBOxidizer)) {
			for (FPPipeConnectionDefinition pipeConn : fb.pipeConnections) {
				List<FPWorkingVisualisation> workVis = new ArrayList<>();
				for (String wvName : pipeConn.enableWorkingVisualisations) {
					FPWorkingVisualisation wv = wvLookup.get(wvName);
					if (wv == null) {
						continue;
					}

					bind.workingVisualisation(wv).conditional((map, entity) -> {
						Direction dir = entity.getDirection();
						Direction connDir = pipeConn.direction.get();
						MapPosition connPos = MapPosition.convert(pipeConn.position.get());
						Direction facing = connDir.rotate(dir);
						MapPosition point = facing.offset(dir.rotate(connPos).add(entity.getPosition()), 1);
						return map.isPipe(point, facing);
					});
				}
			}
		}
	}
}
