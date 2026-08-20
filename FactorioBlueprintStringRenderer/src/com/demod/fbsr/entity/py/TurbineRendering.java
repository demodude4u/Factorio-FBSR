package com.demod.fbsr.entity.py;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.entity.SolarPanelRendering;

// Pyanodon's turbines are solar-panel prototypes now, both the blank placeholders and the
// entities their placeable_by items build, so matching on the old type rejected the profile.
@EntityType(value = "solar-panel", modded = true)
public class TurbineRendering extends SolarPanelRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		String realEntity = lua.get("placeable_by").get("item").tojstring();
		super.defineEntity(bind, prototype.getTable().getEntity(realEntity).get().lua());
	}

}
