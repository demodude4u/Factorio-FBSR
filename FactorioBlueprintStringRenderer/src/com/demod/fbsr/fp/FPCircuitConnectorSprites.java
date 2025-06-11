package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;

public class FPCircuitConnectorSprites {
	public final Optional<FPSprite> connectorMain;
	public final Optional<FPSprite> connectorShadow;
	public final Optional<FPSprite> wirePins;
	public final Optional<FPSprite> wirePinsShadow;

	public FPCircuitConnectorSprites(Profile profile, LuaValue lua) {
		connectorMain = FPUtils.opt(profile, lua.get("connector_main"), FPSprite::new);
		connectorShadow = FPUtils.opt(profile, lua.get("connector_shadow"), FPSprite::new);
		wirePins = FPUtils.opt(profile, lua.get("wire_pins"), FPSprite::new);
		wirePinsShadow = FPUtils.opt(profile, lua.get("wire_pins_shadow"), FPSprite::new);
	}
}
