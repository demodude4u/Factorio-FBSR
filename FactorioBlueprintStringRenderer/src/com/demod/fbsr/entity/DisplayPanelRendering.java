package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSDisplayPanelEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapRenderable;

public class DisplayPanelRendering extends SimpleEntityRendering {

	// TODO parse entity control_behavior, icon, always_show, text, etc...

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSDisplayPanelEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.icon.isPresent() && map.isAltMode()) {
			String item = bsEntity.icon.get().name;

			String itemName = item;
			Optional<ImageDef> icon = TagManager.lookup("item", itemName);
			if (icon.isPresent()) {
				register.accept(new MapIcon(entity.getPosition().addUnit(0, -0.25), icon.get(), 0.5, -0.25, false));
			}
		}
	}

	@Override
	public void defineEntity(SimpleEntityRendering.Bindings bind, LuaTable lua) {
		bind.sprite4Way(lua.get("sprites"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSDisplayPanelEntity.class;
	}

}
