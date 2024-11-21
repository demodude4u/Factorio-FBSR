package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.fp.FPRotatedAnimation8Way;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;

public abstract class SimpleEntityRendering extends EntityRendererFactory {

	public static abstract class BindAction<T> {
		protected T proto;
		protected Layer layer = Layer.ENTITY;

		public abstract List<Sprite> createSprites(BlueprintEntity entity);

		public BindAction<T> layer(Layer layer) {
			this.layer = layer;
			return this;
		}
	}

	public static abstract class BindDirAction<T> extends BindAction<T> {
		protected Optional<Direction> direction = Optional.empty();

		public BindDirAction<T> direction(Direction direction) {
			this.direction = Optional.of(direction);
			return this;
		}

		@Override
		public BindDirAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}
	}

	public static abstract class BindDirFrameAction<T> extends BindAction<T> {
		protected Optional<Direction> direction = Optional.empty();
		protected int frame = 0;

		public BindDirFrameAction<T> direction(Direction direction) {
			this.direction = Optional.of(direction);
			return this;
		}

		public BindDirFrameAction<T> frame(int frame) {
			this.frame = frame;
			return this;
		}

		@Override
		public BindDirFrameAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}
	}

	public static abstract class BindFrameAction<T> extends BindAction<T> {
		protected int frame = 0;

		public BindFrameAction<T> frame(int frame) {
			this.frame = frame;
			return this;
		}

		@Override
		public BindFrameAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}
	}

	public class Bindings {
		private final List<BindAction<?>> bindings;

		public Bindings(List<BindAction<?>> bindings) {
			this.bindings = bindings;
		}

		public BindFrameAction<FPAnimation> animation(LuaValue lua) {
			BindFrameAction<FPAnimation> ret = new BindFrameAction<FPAnimation>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					return proto.createSprites(frame);
				}
			};
			ret.proto = new FPAnimation(lua);
			bindings.add(ret);
			return ret;
		}

		public BindDirFrameAction<FPAnimation4Way> animation4Way(LuaValue lua) {
			BindDirFrameAction<FPAnimation4Way> ret = new BindDirFrameAction<FPAnimation4Way>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					return proto.createSprites(direction.orElse(entity.getDirection()), frame);
				}
			};
			ret.proto = new FPAnimation4Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateFrameAction<FPRotatedAnimation> rotatedAnimation(LuaValue lua) {
			BindRotateFrameAction<FPRotatedAnimation> ret = new BindRotateFrameAction<FPRotatedAnimation>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					double orientation = this.orientation.orElse(entity.getDirection().getOrientation());
					if (entity.json().has("orientation")) {
						orientation = entity.json().getDouble("orientation");
					}
					return proto.createSprites(orientation, frame);
				}
			};
			ret.proto = new FPRotatedAnimation(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateDirFrameAction<FPRotatedAnimation8Way> rotatedAnimation8Way(LuaValue lua) {
			BindRotateDirFrameAction<FPRotatedAnimation8Way> ret = new BindRotateDirFrameAction<FPRotatedAnimation8Way>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					double orientation = this.orientation.orElse(entity.getDirection().getOrientation());
					if (entity.json().has("orientation")) {
						orientation = entity.json().getDouble("orientation");
					}
					return proto.createSprites(direction.orElse(entity.getDirection()), orientation, frame);
				}
			};
			ret.proto = new FPRotatedAnimation8Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateAction<FPRotatedSprite> rotatedSprite(LuaValue lua) {
			BindRotateAction<FPRotatedSprite> ret = new BindRotateAction<FPRotatedSprite>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					double orientation = this.orientation.orElse(entity.getDirection().getOrientation());
					if (entity.json().has("orientation")) {
						orientation = entity.json().getDouble("orientation");
					}
					return proto.createSprites(orientation);
				}
			};
			ret.proto = new FPRotatedSprite(lua);
			bindings.add(ret);
			return ret;
		}

		public BindAction<FPSprite> sprite(LuaValue lua) {
			BindAction<FPSprite> ret = new BindAction<FPSprite>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					return proto.createSprites();
				}
			};
			ret.proto = new FPSprite(lua);
			bindings.add(ret);
			return ret;
		}

		public BindDirAction<FPSprite4Way> sprite4Way(LuaValue lua) {
			BindDirAction<FPSprite4Way> ret = new BindDirAction<FPSprite4Way>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					return proto.createSprites(direction.orElse(entity.getDirection()));
				}
			};
			ret.proto = new FPSprite4Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindVarAction<FPSpriteVariations> spriteVariations(LuaValue lua) {
			BindVarAction<FPSpriteVariations> ret = new BindVarAction<FPSpriteVariations>() {
				@Override
				public List<Sprite> createSprites(BlueprintEntity entity) {
					return proto.createSprites(variation);
				}
			};
			ret.proto = new FPSpriteVariations(lua);
			bindings.add(ret);
			return ret;
		}
	}

	public static abstract class BindRotateAction<T> extends BindAction<T> {
		protected OptionalDouble orientation = OptionalDouble.empty();

		@Override
		public BindRotateAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}

		public BindRotateAction<T> orientation(double orientation) {
			this.orientation = OptionalDouble.of(orientation);
			return this;
		}
	}

	public static abstract class BindRotateDirFrameAction<T> extends BindAction<T> {
		protected OptionalDouble orientation = OptionalDouble.empty();
		protected int frame = 0;
		protected Optional<Direction> direction = Optional.empty();

		public BindRotateDirFrameAction<T> direction(Direction direction) {
			this.direction = Optional.of(direction);
			return this;
		}

		public BindRotateDirFrameAction<T> frame(int frame) {
			this.frame = frame;
			return this;
		}

		@Override
		public BindRotateDirFrameAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}

		public BindRotateDirFrameAction<T> orientation(double orientation) {
			this.orientation = OptionalDouble.of(orientation);
			return this;
		}
	}

	public static abstract class BindRotateFrameAction<T> extends BindAction<T> {
		protected OptionalDouble orientation = OptionalDouble.empty();
		protected int frame = 0;

		public BindRotateFrameAction<T> frame(int frame) {
			this.frame = frame;
			return this;
		}

		@Override
		public BindRotateFrameAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}

		public BindRotateFrameAction<T> orientation(double orientation) {
			this.orientation = OptionalDouble.of(orientation);
			return this;
		}
	}

	public static abstract class BindVarAction<T> extends BindAction<T> {
		protected int variation = 0;

		@Override
		public BindVarAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}

		public BindVarAction<T> variation(int variation) {
			this.variation = variation;
			return this;
		}
	}

	private List<BindAction<?>> bindings;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		for (BindAction<?> bindAction : bindings) {
			register.accept(RenderUtils.spriteRenderer(bindAction.layer, bindAction.createSprites(entity), entity,
					protoSelectionBox));
		}
	}

	public abstract void defineEntity(Bindings bind, LuaValue lua);

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		List<BindAction<?>> bindings = new ArrayList<>();
		Bindings fluent = new Bindings(bindings);
		defineEntity(fluent, prototype.lua());
		this.bindings = bindings;
	}

}