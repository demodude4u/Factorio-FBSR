package demod.fbsr.render;

import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;

public class BeaconRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		Sprite baseSprite = getSpriteFromAnimation(prototype.lua().get("base_picture"));
		Sprite antennaSpriteShadow = getSpriteFromAnimation(prototype.lua().get("animation_shadow"));
		Sprite antennaSprite = getSpriteFromAnimation(prototype.lua().get("animation"));
		register.accept(spriteRenderer(baseSprite, entity, prototype));
		register.accept(spriteRenderer(antennaSpriteShadow, entity, prototype));
		register.accept(spriteRenderer(antennaSprite, entity, prototype));
	}
}
