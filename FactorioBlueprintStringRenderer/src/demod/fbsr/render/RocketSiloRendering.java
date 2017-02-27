package demod.fbsr.render;

import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;
import demod.fbsr.render.Renderer.Layer;

public class RocketSiloRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		Sprite baseSprite = getSpriteFromAnimation(prototype.lua().get("base_day_sprite"));
		Sprite shadowSprite = getSpriteFromAnimation(prototype.lua().get("shadow_sprite"));
		Sprite doorFrontSprite = getSpriteFromAnimation(prototype.lua().get("door_front_sprite"));
		Sprite doorBackSprite = getSpriteFromAnimation(prototype.lua().get("door_back_sprite"));

		register.accept(spriteRenderer(Layer.ENTITY, shadowSprite, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY, doorFrontSprite, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY, doorBackSprite, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY2, baseSprite, entity, prototype));
	}
}
