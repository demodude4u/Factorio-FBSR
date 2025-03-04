package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPCargoBayConnectableGraphicsSet {
	public final Optional<FPLayeredSprite> picture;
	public final Optional<FPAnimation> animation;
	public final Optional<FPCargoBayConnections> connections;

	public FPCargoBayConnectableGraphicsSet(LuaValue lua) {
		picture = FPUtils.opt(lua.get("picture"), FPLayeredSprite::new);
		animation = FPUtils.opt(lua.get("animation"), FPAnimation::new);
		connections = FPUtils.opt(lua.get("connections"), FPCargoBayConnections::new);
	}
}