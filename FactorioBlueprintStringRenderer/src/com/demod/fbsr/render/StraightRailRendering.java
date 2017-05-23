package com.demod.fbsr.render;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;

public class StraightRailRendering extends TypeRendererFactory {

	private static final String[] railNames = { //
			"straight_rail_vertical", //
			"straight_rail_diagonal_right_top", //
			"straight_rail_horizontal", //
			"straight_rail_diagonal_right_bottom", //
			"straight_rail_vertical", //
			"straight_rail_diagonal_left_bottom", //
			"straight_rail_horizontal", //
			"straight_rail_diagonal_left_top", //
	};

	public static final LinkedHashMap<String, Layer> railLayers = new LinkedHashMap<>();
	static {
		railLayers.put("stone_path_background", Layer.RAIL_STONE_BACKGROUND);
		railLayers.put("stone_path", Layer.RAIL_STONE);
		railLayers.put("ties", Layer.RAIL_TIES);
		railLayers.put("backplates", Layer.RAIL_BACKPLATES);
		railLayers.put("metals", Layer.RAIL_METALS);
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {

		String railName = railNames[entity.getDirection().ordinal()];
		LuaValue pictureRailLua = prototype.lua().get("pictures").get(railName);
		for (Entry<String, Layer> entry : railLayers.entrySet()) {
			Sprite railLayerSprite = getSpriteFromAnimation(pictureRailLua.get(entry.getKey()).get("sheet"));
			register.accept(spriteRenderer(entry.getValue(), railLayerSprite, entity, prototype));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
	}
}
