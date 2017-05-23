package com.demod.fbsr.render;

import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;

public class CurvedRailRendering extends TypeRendererFactory {
	private static final String[] railNames = { //
			"curved_rail_vertical_left_bottom", //
			"curved_rail_vertical_right_bottom", //
			"curved_rail_horizontal_left_top", //
			"curved_rail_horizontal_left_bottom", //
			"curved_rail_vertical_right_top", //
			"curved_rail_vertical_left_top", //
			"curved_rail_horizontal_right_bottom", //
			"curved_rail_horizontal_right_top", //
	};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		String railName = railNames[entity.getDirection().ordinal()];
		LuaValue pictureRailLua = prototype.lua().get("pictures").get(railName);
		for (Entry<String, Layer> entry : StraightRailRendering.railLayers.entrySet()) {
			Sprite railLayerSprite = getSpriteFromAnimation(pictureRailLua.get(entry.getKey()).get("sheet"));
			register.accept(spriteRenderer(entry.getValue(), railLayerSprite, entity, prototype));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
	}
}
