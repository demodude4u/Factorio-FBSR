package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;

public class FPCircuitConnectorSprites {
	public final Optional<FPSprite> connectorMain;
	public final Optional<FPSprite> connectorShadow;
	public final Optional<FPSprite> wirePins;
	public final Optional<FPSprite> wirePinsShadow;

	public FPCircuitConnectorSprites(LuaValue lua) {
		connectorMain = FPUtils.opt(lua.get("connector_main"), FPSprite::new);
		connectorShadow = FPUtils.opt(lua.get("connector_shadow"), FPSprite::new);
		wirePins = FPUtils.opt(lua.get("wire_pins"), FPSprite::new);
		wirePinsShadow = FPUtils.opt(lua.get("wire_pins_shadow"), FPSprite::new);
	}
}
