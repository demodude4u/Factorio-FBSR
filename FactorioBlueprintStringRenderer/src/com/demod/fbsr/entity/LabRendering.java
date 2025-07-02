package com.demod.fbsr.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.LogisticGridCell;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("lab")
public class LabRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation(lua.get("off_animation"));
	}
}
