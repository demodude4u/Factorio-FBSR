package com.demod.fbsr.entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class RollingStockRendering extends EntityRendererFactory {
	protected static class RotationSprites {
		private static double PROJECTION_CONSTANT = 0.7071067811865;

		public List<SpriteDef> sprites;
		public int directionCount;
		public boolean backEqualsFront;

		public int getRotationIndex(double orientation) {
			if (backEqualsFront) {
				directionCount *= 2;
			}
			int index = (int) (projectedFraction(orientation) * directionCount);
			if (backEqualsFront) {
				index = index % (directionCount / 2);
			}
			return index;
		}

		public SpriteDef getSprite(double orientation) {
			return sprites.get(getRotationIndex(orientation));
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

	protected static class TintedRotationSprites extends RotationSprites {
		private final Cache<Long, SpriteDef> tintCache = CacheBuilder.newBuilder().maximumSize(100).build();

		public SpriteDef getSprite(double orientation, Color tint) {
			int index = getRotationIndex(orientation);
			long key = (index << 32) + tint.getRGB();
			SpriteDef sprite = tintCache.getIfPresent(key);
			if (sprite == null) {
				SpriteDef baseSprite = sprites.get(index);
				sprite = baseSprite.withImage(Utils.tintImage(baseSprite.getImage(), tint));
				tintCache.put(key, sprite);
			}
			return sprite;
		}
	}

	protected RotationSprites protoBodySprites;
	protected TintedRotationSprites protoBodyMaskSprites;
	protected RotationSprites protoShadowSprites;
	protected RotationSprites protoWheelSprites;
	private double protoJointDistance;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		double orientation = entity.json().getDouble("orientation");

		SpriteDef sprite = protoBodySprites.getSprite(orientation);

		SpriteDef mask;
		if (protoBodyMaskSprites != null && entity.json().has("color")) {
			Color color = RenderUtils.parseColor(entity.json().getJSONObject("color"));
			mask = protoBodyMaskSprites.getSprite(orientation, color);
		} else {
			mask = null;
		}

		SpriteDef spriteShadow = protoShadowSprites.getSprite(orientation);

		double rotation = orientation * Math.PI * 2 + Math.PI * 0.5;
		double dx = (protoJointDistance / 2.0) * Math.cos(rotation);
		double dy = (protoJointDistance / 2.0) * Math.sin(rotation);
		double railShift = 0.25 * Math.abs(Math.cos(rotation));

		Sprite spriteWheels1 = protoWheelSprites.getSprite(orientation).createSprite();
		spriteWheels1.bounds.x += -dx;
		spriteWheels1.bounds.y += -dy - railShift;

		double orientation180 = orientation < 0.5 ? orientation + 0.5 : orientation - 0.5;
		Sprite spriteWheels2 = protoWheelSprites.getSprite(orientation180).createSprite();
		spriteWheels2.bounds.x += dx;
		spriteWheels2.bounds.y += dy - railShift;

		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY2, spriteShadow, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, spriteWheels1, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, spriteWheels2, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY2, sprite, entity, protoSelectionBox));
		if (mask != null) {
			register.accept(RenderUtils.spriteDefRenderer(Layer.ENTITY2, mask, entity, protoSelectionBox));
		}
	}

	protected RotationSprites getRotatedSprites(LuaValue lua) {
		return getRotatedSprites(lua, RotationSprites::new);
	}

	protected <T extends RotationSprites> T getRotatedSprites(LuaValue lua, Supplier<T> factory) {
		T ret = factory.get();
		LuaValue hrVersion = lua.get("hr_version");
		if (!hrVersion.isnil()) {
			lua = hrVersion;
		}

		ret.sprites = new ArrayList<>();
		ret.backEqualsFront = lua.get("back_equals_front").optboolean(false);
		ret.directionCount = lua.get("direction_count").toint();

		int lineLength = lua.get("line_length").toint();
		int linesPerFile = lua.get("lines_per_file").toint();
		int fileLength = lineLength * linesPerFile;

		for (int i = 0; i < ret.directionCount; i++) {
			int fileIndex = i / fileLength;
			int tileIndex = i % fileLength;
			SpriteDef sprite = RenderUtils.getSpriteFromAnimation(lua, fileIndex + 1).get();
			// XXX Violation of immutability
			sprite.getSource().x += (tileIndex % lineLength) * sprite.getSource().width;
			sprite.getSource().y += (tileIndex / lineLength) * sprite.getSource().height;
			ret.sprites.add(sprite);
		}
		return ret;
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue layers = prototype.lua().get("pictures").get("layers");

		LuaValue body = layers.get(1);
		protoBodySprites = getRotatedSprites(body);

		if (layers.length() == 3) {
			LuaValue bodyMask = layers.get(2);
			protoBodyMaskSprites = getRotatedSprites(bodyMask, TintedRotationSprites::new);
		} else {
			protoBodyMaskSprites = null;
		}

		LuaValue shadow = layers.get(layers.length());
		protoShadowSprites = getRotatedSprites(shadow);

		LuaValue wheels = prototype.lua().get("wheels");
		protoWheelSprites = getRotatedSprites(wheels);

		protoJointDistance = prototype.lua().get("joint_distance").todouble();
	}
}
