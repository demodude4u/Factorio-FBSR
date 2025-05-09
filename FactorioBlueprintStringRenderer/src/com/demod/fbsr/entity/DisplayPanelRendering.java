package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSSignalID;
import com.demod.fbsr.bs.entity.BSDisplayPanelEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapRenderable;

public class DisplayPanelRendering extends EntityWithOwnerRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSDisplayPanelEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.icon.isPresent() && map.isAltMode()) {
			BSSignalID signalID = bsEntity.icon.get();

			Optional<IconDefWithQuality> icon = IconManager.lookupSignalID(signalID.type, signalID.name,
					signalID.quality);
			if (icon.isPresent()) {
				register.accept(new MapIcon(entity.getPosition().addUnit(0, -0.25), icon.get().getDef(), 0.5,
						OptionalDouble.empty(), false, signalID.quality.filter(s -> !s.equals("normal"))));
			}
		}
	}

	@Override
	public void defineEntity(EntityRendering.Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.sprite4Way(lua.get("sprites"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSDisplayPanelEntity.class;
	}

}
