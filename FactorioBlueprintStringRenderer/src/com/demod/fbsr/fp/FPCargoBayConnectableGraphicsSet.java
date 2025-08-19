package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;

public class FPCargoBayConnectableGraphicsSet {
	public final Optional<FPLayeredSprite> picture;
	public final Optional<FPAnimation> animation;
	public final Optional<FPCargoBayConnections> connections;

	public FPCargoBayConnectableGraphicsSet(Profile profile, LuaValue lua) {
		picture = FPUtils.opt(profile, lua.get("picture"), FPLayeredSprite::new);
		animation = FPUtils.opt(profile, lua.get("animation"), FPAnimation::new);
		connections = FPUtils.opt(profile, lua.get("connections"), FPCargoBayConnections::new);
	}
}