package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import java.util.function.Consumer;

import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapText;
import com.demod.fbsr.map.MapUnknownTileMarker;

public class UnknownTileRendering extends TileRendererFactory {
	public static final Font FONT = GUIStyle.FONT_BP_BOLD.deriveFont(0.4f);

	private final Color color;
	private final float offset;

	public UnknownTileRendering(String name) {
		this.name = name;
		Random random = new Random(name.hashCode());
		color = RenderUtils.withAlpha(Color.getHSBColor(random.nextFloat(), 0.6f, 0.4f), 128);
		offset = random.nextFloat();
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, BSTile tile) {
		MapPosition pos = tile.position.createPoint();
		register.accept(new MapUnknownTileMarker(pos, color));
		if (map.addUnknownTile(name)) {
			register.accept(new MapText(Layer.ENTITY_INFO_TEXT, pos.addUnit(-0.25, -0.25 + offset), null, color, name));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, BSTile tile) {
	}

	@Override
	public boolean isUnknown() {
		return true;
	}
}
