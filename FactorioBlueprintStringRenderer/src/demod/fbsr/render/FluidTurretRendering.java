package demod.fbsr.render;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.BlueprintEntity.Direction;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;

public class FluidTurretRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		Sprite baseSprite = getSpriteFromAnimation(prototype.lua().get("base_picture")
				.get(entity.getDirection().name().toLowerCase()).get("layers").get(1));
		register.accept(spriteRenderer(baseSprite, entity, prototype));
		LuaValue turretLayers = prototype.lua().get("folded_animation").get(entity.getDirection().name().toLowerCase())
				.get("layers");
		Sprite turretSprite = getSpriteFromAnimation(turretLayers.get(1));
		register.accept(spriteRenderer(turretSprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		Direction dir = entity.getDirection();
		map.setPipe(dir.right().offset(dir.back().offset(entity.getPosition()), 0.5), dir.right());
		map.setPipe(dir.left().offset(dir.back().offset(entity.getPosition()), 0.5), dir.left());
	}
}
