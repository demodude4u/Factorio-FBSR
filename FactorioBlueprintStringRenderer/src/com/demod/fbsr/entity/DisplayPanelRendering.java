package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Font;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSSignalID;
import com.demod.fbsr.bs.entity.BSDisplayPanelEntity;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapText;

public class DisplayPanelRendering extends EntityWithOwnerRendering {

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSDisplayPanelEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.icon.isPresent() && map.isAltMode()) {
			BSSignalID signalID = bsEntity.icon.get();

			Optional<IconDefWithQuality> icon = profile.getIconManager().lookupSignalID(signalID.type, signalID.name,
					signalID.quality);
			if (icon.isPresent()) {
				register.accept(new MapIcon(entity.getPosition().addUnit(0, -0.25), icon.get().getDef(), 0.5,
						OptionalDouble.empty(), false, signalID.quality.filter(s -> !s.equals("normal"))));
			}
		}

		if (bsEntity.text.isPresent() && bsEntity.text.get().length > 0 && map.isAltMode()) {

			Font font = profile.getGuiStyle().FONT_BP_BOLD.deriveFont(0.3f);
			register.accept(new MapText(Layer.ENTITY_INFO_TEXT, entity.getPosition().addUnit(0, -0.75), 0, font, Color.white, bsEntity.text.get()[0], true));
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
