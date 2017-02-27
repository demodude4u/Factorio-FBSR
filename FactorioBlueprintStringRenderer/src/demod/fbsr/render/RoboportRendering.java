package demod.fbsr.render;

import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;

public class RoboportRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		register.accept(spriteRenderer(getSpriteFromAnimation(prototype.lua().get("base")), entity, prototype));
		register.accept(
				spriteRenderer(getSpriteFromAnimation(prototype.lua().get("door_animation_down")), entity, prototype));
		register.accept(
				spriteRenderer(getSpriteFromAnimation(prototype.lua().get("door_animation_up")), entity, prototype));
	}
}
