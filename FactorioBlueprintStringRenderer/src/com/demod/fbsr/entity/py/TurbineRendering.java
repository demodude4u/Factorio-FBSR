package com.demod.fbsr.entity.py;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.entity.ElectricEnergyInterfaceRendering;
import com.demod.fbsr.entity.SimpleEntityRendering;

public class TurbineRendering extends ElectricEnergyInterfaceRendering {

	@Override
	public void defineEntity(SimpleEntityRendering.Bindings bind, LuaTable lua) {
		String realEntity = lua.get("placeable_by").get("item").tojstring();
		super.defineEntity(bind, data.getTable().getEntity(realEntity).get().lua());
	}

}
