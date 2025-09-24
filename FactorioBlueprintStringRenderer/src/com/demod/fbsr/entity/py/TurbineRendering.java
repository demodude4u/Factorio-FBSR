package com.demod.fbsr.entity.py;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.entity.ElectricEnergyInterfaceRendering;

@EntityType(value = "electric-energy-interface", modded = true)
public class TurbineRendering extends ElectricEnergyInterfaceRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		String realEntity = lua.get("placeable_by").get("item").tojstring();
		super.defineEntity(bind, prototype.getTable().getEntity(realEntity).get().lua());
	}

}
