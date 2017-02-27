package demod.fbsr.render;

import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;

public class AccumulatorRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("picture"));
		register.accept(spriteRenderer(sprite, entity, prototype));
	}
}
