package com.demod.fbsr.entity.py;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.entity.ElectricEnergyInterfaceRendering;
import com.demod.fbsr.entity.EntityRendering;

public class TurbineRendering extends ElectricEnergyInterfaceRendering {

	@Override
	public void defineEntity(EntityRendering.Bindings bind, LuaTable lua) {
		String realEntity = lua.get("placeable_by").get("item").tojstring();
		super.defineEntity(bind, profile.getData().getTable().getEntity(realEntity).get().lua());
	}

}
