package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class InserterRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		// Utils.debugPrintTable(entity.lua());
		// System.exit(1);

		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("platform_picture").get("sheet"));
		sprite.source.x += sprite.source.width * (entity.getDirection().back().cardinal());

		register.accept(spriteRenderer(sprite, entity, prototype));

		// register.accept(new Renderer(Layer.OVERLAY, entity.getPosition()) {
		// @Override
		// public void render(Graphics2D g) {
		// Point2D.Double pos = entity.getPosition();
		// Direction forward = entity.getDirection().back();
		// Direction left = forward.left();
		//
		// Point2D.Double pickupOffset =
		// LuaUtils.parsePoint2D(prototype.lua().get("pickup_position"));
		// Point2D.Double insertOffset =
		// LuaUtils.parsePoint2D(prototype.lua().get("insert_position"));
		//
		// Point2D.Double pickupPosition = left.offset(forward.offset(pos,
		// pickupOffset.y), pickupOffset.x);
		// Point2D.Double insertPosition = left.offset(forward.offset(pos,
		// insertOffset.y), insertOffset.x);
		//
		// Ellipse2D.Double pickupShape = new Ellipse2D.Double(pickupPosition.x
		// - 0.3, pickupPosition.y - 0.3, 0.6,
		// 0.6);
		// Ellipse2D.Double insertShape = new Ellipse2D.Double(insertPosition.x
		// - 0.2, insertPosition.y - 0.2, 0.4,
		// 0.4);
		//
		// g.setColor(new Color(128, 128, 0, 100));
		// g.fill(pickupShape);
		// g.fill(insertShape);
		// }
		// });
	}

}
