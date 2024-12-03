package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FBSR.EntityRenderingTuple;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteWithLayer;
import com.demod.fbsr.SpritesWithLayer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.fp.FPCircuitConnectorDefinition;
import com.demod.fbsr.fp.FPLayeredSprite;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.fp.FPRotatedAnimation8Way;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public abstract class SimpleEntityRendering extends EntityRendererFactory {

	public static abstract class BindAction<T> {
		protected T proto;

		public abstract List<SpritesWithLayer> createLayeredSprites(BSEntity entity);
	}

	public static abstract class BindDirAction<T> extends BindLayerAction<T> {
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

	public static abstract class BindDirFrameAction<T> extends BindLayerAction<T> {
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

	public static abstract class BindFrameAction<T> extends BindLayerAction<T> {
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
				public List<Sprite> createSprites(BSEntity entity) {
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
				public List<Sprite> createSprites(BSEntity entity) {
					return proto.createSprites(direction.orElse(entity.direction), frame);
				}
			};
			ret.proto = new FPAnimation4Way(lua);
			bindings.add(ret);
			return ret;
		}

		public void circuitConnector(LuaValue lua) {
			if (!lua.isnil()) {
				SimpleEntityRendering.this.circuitConnectors = Optional
						.of(ImmutableList.of(new FPCircuitConnectorDefinition(lua)));
			}
		}

		public void circuitConnector16Way(LuaValue lua) {
			if (!lua.isnil()) {
				List<FPCircuitConnectorDefinition> list = FPUtils.list(lua, FPCircuitConnectorDefinition::new);
				Preconditions.checkArgument(list.size() == 16,
						"Expected 16 circuit connectors, but found " + list.size());
				SimpleEntityRendering.this.circuitConnectors = Optional.of(list);
			}
		}

		public void circuitConnector4Way(LuaValue lua) {
			if (!lua.isnil()) {
				List<FPCircuitConnectorDefinition> list = FPUtils.list(lua, FPCircuitConnectorDefinition::new);
				Preconditions.checkArgument(list.size() == 4,
						"Expected 4 circuit connectors, but found " + list.size());
				SimpleEntityRendering.this.circuitConnectors = Optional.of(list);
			}
		}

		public void circuitConnector8Way(LuaValue lua) {
			if (!lua.isnil()) {
				List<FPCircuitConnectorDefinition> list = FPUtils.list(lua, FPCircuitConnectorDefinition::new);
				Preconditions.checkArgument(list.size() == 8,
						"Expected 8 circuit connectors, but found " + list.size());
				SimpleEntityRendering.this.circuitConnectors = Optional.of(list);
			}
		}

		public void circuitConnectorNWay(LuaValue lua) {
			if (!lua.isnil()) {
				List<FPCircuitConnectorDefinition> list = FPUtils.list(lua, FPCircuitConnectorDefinition::new);
				SimpleEntityRendering.this.circuitConnectors = Optional.of(list);
			}
		}

		public void layeredSprite(LuaValue lua) {
			BindAction<FPLayeredSprite> ret = new BindAction<FPLayeredSprite>() {
				@Override
				public List<SpritesWithLayer> createLayeredSprites(BSEntity entity) {
					return SpriteWithLayer.groupByLayer(proto.createSpritesWithLayers());
				}
			};
			ret.proto = new FPLayeredSprite(lua);
			bindings.add(ret);
		}

		public BindRotateFrameAction<FPRotatedAnimation> rotatedAnimation(LuaValue lua) {
			BindRotateFrameAction<FPRotatedAnimation> ret = new BindRotateFrameAction<FPRotatedAnimation>() {
				@Override
				public List<Sprite> createSprites(BSEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.orientation.isPresent()) {
						orientation = entity.orientation.getAsDouble();
					} else {
						orientation = entity.directionRaw / 16.0;
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
				public List<Sprite> createSprites(BSEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.orientation.isPresent()) {
						orientation = entity.orientation.getAsDouble();
					} else {
						orientation = entity.direction.getOrientation();
					}
					return proto.createSprites(direction.orElse(entity.direction), orientation, frame);
				}
			};
			ret.proto = new FPRotatedAnimation8Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateAction<FPRotatedSprite> rotatedSprite(LuaValue lua) {
			BindRotateAction<FPRotatedSprite> ret = new BindRotateAction<FPRotatedSprite>() {
				@Override
				public List<Sprite> createSprites(BSEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.orientation.isPresent()) {
						orientation = entity.orientation.getAsDouble();
					} else {
						orientation = entity.direction.getOrientation();
					}
					return proto.createSprites(orientation);
				}
			};
			ret.proto = new FPRotatedSprite(lua);
			bindings.add(ret);
			return ret;
		}

		public BindLayerAction<FPSprite> sprite(LuaValue lua) {
			BindLayerAction<FPSprite> ret = new BindLayerAction<FPSprite>() {
				@Override
				public List<Sprite> createSprites(BSEntity entity) {
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
				public List<Sprite> createSprites(BSEntity entity) {
					return proto.createSprites(direction.orElse(entity.direction));
				}
			};
			ret.proto = new FPSprite4Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindVarAction<FPSpriteVariations> spriteVariations(LuaValue lua) {
			BindVarAction<FPSpriteVariations> ret = new BindVarAction<FPSpriteVariations>() {
				@Override
				public List<Sprite> createSprites(BSEntity entity) {
					return proto.createSprites(variation);
				}
			};
			ret.proto = new FPSpriteVariations(lua);
			bindings.add(ret);
			return ret;
		}
	}

	public static abstract class BindLayerAction<T> extends BindAction<T> {
		protected Layer layer = Layer.OBJECT;

		@Override
		public List<SpritesWithLayer> createLayeredSprites(BSEntity entity) {
			return ImmutableList.of(new SpritesWithLayer(layer, createSprites(entity)));
		}

		public abstract List<Sprite> createSprites(BSEntity entity);

		public BindLayerAction<T> layer(Layer layer) {
			this.layer = layer;
			return this;
		}
	}

	public static abstract class BindRotateAction<T> extends BindLayerAction<T> {
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

	public static abstract class BindRotateDirFrameAction<T> extends BindLayerAction<T> {
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

	public static abstract class BindRotateFrameAction<T> extends BindLayerAction<T> {
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

	public static abstract class BindVarAction<T> extends BindLayerAction<T> {
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
	private Optional<List<FPCircuitConnectorDefinition>> circuitConnectors = Optional.empty();

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		for (BindAction<?> bindAction : bindings) {
			List<SpritesWithLayer> sprites = bindAction.createLayeredSprites(entity);
			for (SpritesWithLayer spritesWithLayer : sprites) {
				register.accept(RenderUtils.spriteRenderer(spritesWithLayer.getLayer(), spritesWithLayer.getSprites(),
						entity, protoSelectionBox));
			}
		}
	}

	public abstract void defineEntity(Bindings bind, LuaValue lua);

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaValue lua) {
		if (circuitConnectors.isPresent()) {
			List<FPWireConnectionPoint> points = circuitConnectors.get().stream().map(cc -> cc.points)
					.collect(Collectors.toList());
			consumer.accept(1, WirePoints.fromWireConnectionPoints(points, WireColor.RED, false));
			consumer.accept(2, WirePoints.fromWireConnectionPoints(points, WireColor.GREEN, false));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		List<BindAction<?>> bindings = new ArrayList<>();
		Bindings fluent = new Bindings(bindings);
		defineEntity(fluent, prototype.lua());
		this.bindings = bindings;
	}

	@Override
	public double initWireConnector(Consumer<Renderer> register, BSEntity entity, List<EntityRenderingTuple> wired) {

		if (circuitConnectors.isPresent()) {
			FPCircuitConnectorDefinition circuitConnector = RenderUtils.pickDirectional(circuitConnectors.get(),
					entity);

			circuitConnector.sprites.ifPresent(sprites -> {
				if (sprites.connectorMain.isPresent()) {
					register.accept(RenderUtils.spriteRenderer(sprites.connectorMain.get().createSprites(), entity,
							protoSelectionBox));
				}
				if (sprites.connectorShadow.isPresent()) {
					register.accept(RenderUtils.spriteRenderer(sprites.connectorShadow.get().createSprites(), entity,
							protoSelectionBox));
				}
				if (sprites.wirePins.isPresent()) {
					register.accept(RenderUtils.spriteRenderer(sprites.wirePins.get().createSprites(), entity,
							protoSelectionBox));
				}
				if (sprites.wirePinsShadow.isPresent()) {
					register.accept(RenderUtils.spriteRenderer(sprites.wirePinsShadow.get().createSprites(), entity,
							protoSelectionBox));
				}
			});
		}

		return entity.directionRaw / 16.0;
	}

}
