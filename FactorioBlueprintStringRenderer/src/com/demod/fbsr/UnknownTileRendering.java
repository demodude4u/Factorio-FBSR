package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapText;
import com.demod.fbsr.map.MapTile;
import com.demod.fbsr.map.MapUnknownTileMarker;

public class UnknownTileRendering extends TileRendererFactory {
	private static Font FONT = null;

	private final Color color;
	private final float offset;

	public UnknownTileRendering(String name) {
		this.name = name;
		color = RenderUtils.getUnknownColor(name);
		offset = RenderUtils.getUnknownTextOffset(name);
		unknownModded = true;
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapTile tile) {
		if (FONT == null) {
			FONT = FBSR.getGuiStyle().FONT_BP_BOLD.deriveFont(0.4f);
		}
		
		MapPosition pos = tile.getPosition();
		register.accept(new MapUnknownTileMarker(pos, color));
		if (map.addUnknownTile(name)) {
			register.accept(new MapText(Layer.ENTITY_INFO_TEXT, pos.addUnit(-0.25, -0.25 + offset), 0, FONT,
					Color.white, name, false, tile.getResolver()));
		}
	}

	@Override
	public boolean isUnknown() {
		return true;
	}

	@Override
	public void initFromPrototype(DataTable table) {
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
	}
}
