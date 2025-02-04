package com.demod.fbsr.fp;

import java.util.List;
import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;

public class FPFluidBox {
	public final Optional<String> filter;
	public final Optional<String> productionType;
	public final Optional<FPSprite4Way> pipeCovers;
	public final Optional<FPSprite4Way> pipePicture;
	public final List<FPPipeConnectionDefinition> pipeConnections;

	public FPFluidBox(LuaValue lua) {
		filter = FPUtils.optString(lua.get("filter"));
		productionType = FPUtils.optString(lua.get("production_type"));
		pipeCovers = FPUtils.opt(lua.get("pipe_covers"), FPSprite4Way::new);
		pipePicture = FPUtils.opt(lua.get("pipe_picture"), FPSprite4Way::new);
		pipeConnections = FPUtils.list(lua.get("pipe_connections"), FPPipeConnectionDefinition::new);
	}
}
