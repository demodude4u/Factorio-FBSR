package com.demod.fbsr.entity;

import java.awt.Color;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class RollingStockRendering extends EntityRendererFactory {
	private static double PROJECTION_CONSTANT = 0.7071067811865;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		double orientation = entity.json().getDouble("orientation");

		LuaValue layers = prototype.lua().get("pictures").get("layers");

		LuaValue body = layers.get(1);
		Sprite sprite = getRotatedSprite(body, orientation);

		Sprite mask = null;
		if (layers.length() == 3 && entity.json().has("color")) {
			Color color = RenderUtils.parseColor(entity.json().getJSONObject("color"));

			LuaValue bodyMask = layers.get(2);
			mask = getRotatedSprite(bodyMask, orientation);

			mask.image = Utils.tintImage(mask.image, color);
		}

		LuaValue shadow = layers.get(layers.length());
		Sprite spriteShadow = getRotatedSprite(shadow, orientation);

		double jointDistance = prototype.lua().get("joint_distance").todouble();
		double rotation = orientation * Math.PI * 2 + Math.PI * 0.5;
		double dx = (jointDistance / 2.0) * Math.cos(rotation);
		double dy = (jointDistance / 2.0) * Math.sin(rotation);
		double railShift = 0.25 * Math.abs(Math.cos(rotation));

		LuaValue wheels = prototype.lua().get("wheels");
		Sprite spriteWheels1 = getRotatedSprite(wheels, orientation);
		spriteWheels1.bounds.x += -dx;
		spriteWheels1.bounds.y += -dy - railShift;
		Sprite spriteWheels2 = getRotatedSprite(wheels, orientation < 0.5 ? orientation + 0.5 : orientation - 0.5);
		spriteWheels2.bounds.x += dx;
		spriteWheels2.bounds.y += dy - railShift;

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, spriteShadow, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, spriteWheels1, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, spriteWheels2, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, sprite, entity, prototype));
		if (mask != null) {
			register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, mask, entity, prototype));
		}
	}

	protected Sprite getRotatedSprite(LuaValue lua, double orientation) {
		LuaValue hrVersion = lua.get("hr_version");
		if (!hrVersion.isnil()) {
			lua = hrVersion;
		}

		int lineLength = lua.get("line_length").toint();
		int linesPerFile = lua.get("lines_per_file").toint();
		int fileLength = lineLength * linesPerFile;

		int index = getRotationIndex(lua, orientation);
		int fileIndex = index / fileLength;
		int tileIndex = index % fileLength;
		lua.set("filename_selector", fileIndex + 1); // XXX
		Sprite sprite = RenderUtils.getSpriteFromAnimation(lua);
		sprite.source.x += (tileIndex % lineLength) * sprite.source.width;
		sprite.source.y += (tileIndex / lineLength) * sprite.source.height;
		return sprite;
	}

	protected int getRotationIndex(LuaValue lua, double orientation) {
		boolean backEqualsFront = lua.get("back_equals_front").optboolean(false);
		int directionCount = lua.get("direction_count").toint();
		if (backEqualsFront) {
			directionCount *= 2;
		}
		int index = (int) (projectedFraction(orientation) * directionCount);
		if (backEqualsFront) {
			index = index % (directionCount / 2);
		}
		return index;
	}

	private double projectedFraction(double orientation) {
		if (orientation == 0 || orientation == 0.25 || orientation == 0.5 || orientation == 0.75)
			return orientation;
		if (orientation < 0.5)
			if (orientation < 0.25) {
				double ratio = Math.tan(orientation * 2 * Math.PI);
				ratio *= PROJECTION_CONSTANT;
				return Math.atan(ratio) / 2.0 / Math.PI;
			} else {
				double ratio = Math.tan((orientation - 0.25) * 2 * Math.PI);
				ratio *= 1 / PROJECTION_CONSTANT;
				return Math.atan(ratio) / 2.0 / Math.PI + 0.25;
			}
		else if (orientation < 0.75) {
			double ratio = Math.tan((0.75 - orientation) * 2 * Math.PI);
			ratio *= 1 / PROJECTION_CONSTANT;
			return 0.75 - Math.atan(ratio) / 2.0 / Math.PI;
		} else {
			double ratio = Math.tan((orientation - 0.75) * 2 * Math.PI);
			ratio *= 1 / PROJECTION_CONSTANT;
			return Math.atan(ratio) / 2.0 / Math.PI + 0.75;
		}
	}
}
