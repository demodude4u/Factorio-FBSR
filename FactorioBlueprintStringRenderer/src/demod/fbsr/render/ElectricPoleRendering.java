package demod.fbsr.render;

import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;
import demod.fbsr.render.Renderer.Layer;

public class ElectricPoleRendering extends TypeRendererFactory {

	private static final int SpriteIndex = 2;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		super.createRenderers(register, map, dataTable, entity, prototype);
		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("pictures"));
		sprite.source.x = sprite.source.width * SpriteIndex;
		register.accept(spriteRenderer(Layer.ENTITY3, sprite, entity, prototype));
	}
}
