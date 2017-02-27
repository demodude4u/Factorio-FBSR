package demod.fbsr.render;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.BlueprintEntity.Direction;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.WorldMap;

public class GeneratorRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		debugPrintContext(entity, prototype);

		Sprite sprite = getSpriteFromAnimation(prototype.lua()
				.get((entity.getDirection().cardinal() % 2) == 0 ? "vertical_animation" : "horizontal_animation"));
		register.accept(spriteRenderer(sprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		Direction dir = entity.getDirection();
		Point2D.Double position = entity.getPosition();
		map.setPipe(dir.offset(position, 2), dir);
		map.setPipe(dir.back().offset(position, 2), dir.back());
	}
}
