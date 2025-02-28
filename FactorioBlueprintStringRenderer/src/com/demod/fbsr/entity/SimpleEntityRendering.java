package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.MapUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.SimpleEntityRendering.BindDirAction.BindDirNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindDirFrameAction.BindDirFrameNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindFrameAction.BindFrameNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindLayerAction.BindLayerNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindRotateAction.BindRotateNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindRotateDirFrameAction.BindRotateDirFrameNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindRotateFrameAction.BindRotateFrameNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindVarAction.BindVarNoAction;
import com.demod.fbsr.entity.SimpleEntityRendering.BindVarFrameAction.BindVarFrameNoAction;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.fp.FPAnimationVariations;
import com.demod.fbsr.fp.FPCircuitConnectorDefinition;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPLayeredSprite;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.fp.FPRotatedAnimation8Way;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

//TODO needs to be renamed not to be confused with SimpleEntityPrototype...
public abstract class SimpleEntityRendering extends EntityRendererFactory {

	public static abstract class BindAction<T> {
		public static class BindNoAction<T> extends BindAction<T> {
			@Override
			public List<LayeredSpriteDef> defineLayeredSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

		protected T proto;

		public abstract List<LayeredSpriteDef> defineLayeredSprites(BSEntity entity);
	}

	public static abstract class BindDirAction<T> extends BindLayerAction<T> {
		public static class BindDirNoAction<T> extends BindDirAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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
		public static class BindDirFrameNoAction<T> extends BindDirFrameAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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
		public static class BindFrameNoAction<T> extends BindFrameAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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
			if (lua.isnil()) {
				return new BindFrameNoAction<>();
			}
			BindFrameAction<FPAnimation> ret = new BindFrameAction<FPAnimation>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					return proto.defineSprites(frame);
				}
			};
			ret.proto = new FPAnimation(lua);
			bindings.add(ret);
			return ret;
		}

		public BindDirFrameAction<FPAnimation4Way> animation4Way(LuaValue lua) {
			if (lua.isnil()) {
				return new BindDirFrameNoAction<>();
			}
			BindDirFrameAction<FPAnimation4Way> ret = new BindDirFrameAction<FPAnimation4Way>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					return proto.defineSprites(direction.orElse(entity.direction), frame);
				}
			};
			ret.proto = new FPAnimation4Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindVarFrameAction<FPAnimationVariations> animationVariations(LuaValue lua) {
			if (lua.isnil()) {
				return new BindVarFrameNoAction<>();
			}
			BindVarFrameAction<FPAnimationVariations> ret = new BindVarFrameAction<FPAnimationVariations>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					return proto.defineSprites(variation, frame);
				}
			};
			ret.proto = new FPAnimationVariations(lua);
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

		public void fluidBox(LuaValue lua) {
			if (!lua.isnil()) {
				SimpleEntityRendering.this.fluidBoxes.add(new FPFluidBox(lua));
			}
		}

		public void fluidBoxes(LuaValue lua) {
			if (!lua.isnil()) {
				SimpleEntityRendering.this.fluidBoxes.addAll(FPUtils.list(lua, FPFluidBox::new));
			}
		}

		public void layeredSprite(LuaValue lua) {
			BindAction<FPLayeredSprite> ret = new BindAction<FPLayeredSprite>() {
				@Override
				public List<LayeredSpriteDef> defineLayeredSprites(BSEntity entity) {
					return proto.defineLayeredSprites();
				}
			};
			ret.proto = new FPLayeredSprite(lua);
			bindings.add(ret);
		}

		public BindRotateFrameAction<FPRotatedAnimation> rotatedAnimation(LuaValue lua) {
			if (lua.isnil()) {
				return new BindRotateFrameNoAction<>();
			}
			BindRotateFrameAction<FPRotatedAnimation> ret = new BindRotateFrameAction<FPRotatedAnimation>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.orientation.isPresent()) {
						orientation = entity.orientation.getAsDouble();
					} else {
						orientation = entity.directionRaw / 16.0;
					}
					return proto.defineSprites(orientation, frame);
				}
			};
			ret.proto = new FPRotatedAnimation(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateDirFrameAction<FPRotatedAnimation8Way> rotatedAnimation8Way(LuaValue lua) {
			if (lua.isnil()) {
				return new BindRotateDirFrameNoAction<>();
			}
			BindRotateDirFrameAction<FPRotatedAnimation8Way> ret = new BindRotateDirFrameAction<FPRotatedAnimation8Way>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.orientation.isPresent()) {
						orientation = entity.orientation.getAsDouble();
					} else {
						orientation = entity.direction.getOrientation();
					}
					return proto.defineSprites(direction.orElse(entity.direction), orientation, frame);
				}
			};
			ret.proto = new FPRotatedAnimation8Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateAction<FPRotatedSprite> rotatedSprite(LuaValue lua) {
			if (lua.isnil()) {
				return new BindRotateNoAction<>();
			}
			BindRotateAction<FPRotatedSprite> ret = new BindRotateAction<FPRotatedSprite>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.orientation.isPresent()) {
						orientation = entity.orientation.getAsDouble();
					} else {
						orientation = entity.direction.getOrientation();
					}
					return proto.defineSprites(orientation);
				}
			};
			ret.proto = new FPRotatedSprite(lua);
			bindings.add(ret);
			return ret;
		}

		public BindLayerAction<FPSprite> sprite(LuaValue lua) {
			if (lua.isnil()) {
				return new BindLayerNoAction<>();
			}
			BindLayerAction<FPSprite> ret = new BindLayerAction<FPSprite>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					return proto.defineSprites();
				}
			};
			ret.proto = new FPSprite(lua);
			bindings.add(ret);
			return ret;
		}

		public BindDirAction<FPSprite4Way> sprite4Way(LuaValue lua) {
			if (lua.isnil()) {
				return new BindDirNoAction<>();
			}
			BindDirAction<FPSprite4Way> ret = new BindDirAction<FPSprite4Way>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					return proto.defineSprites(direction.orElse(entity.direction));
				}
			};
			ret.proto = new FPSprite4Way(lua);
			bindings.add(ret);
			return ret;
		}

		public BindVarAction<FPSpriteVariations> spriteVariations(LuaValue lua) {
			if (lua.isnil()) {
				return new BindVarNoAction<>();
			}
			BindVarAction<FPSpriteVariations> ret = new BindVarAction<FPSpriteVariations>() {
				@Override
				public List<SpriteDef> defineSprites(BSEntity entity) {
					return proto.defineSprites(variation);
				}
			};
			ret.proto = new FPSpriteVariations(lua);
			bindings.add(ret);
			return ret;
		}
	}

	public static abstract class BindLayerAction<T> extends BindAction<T> {
		public static class BindLayerNoAction<T> extends BindLayerAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

		protected Layer layer = Layer.OBJECT;
		protected Optional<FPVector> offset = Optional.empty();

		@Override
		public List<LayeredSpriteDef> defineLayeredSprites(BSEntity entity) {
			// XXX I don't like these transient instances
			List<LayeredSpriteDef> sprites = defineSprites(entity).stream().map(s -> new LayeredSpriteDef(s, layer))
					.collect(Collectors.toList());
			if (offset.isPresent()) {
				MapPosition offset = MapPosition.convert(this.offset.get());
				for (LayeredSpriteDef sprite : sprites) {
					sprite.offset(offset);
				}
			}
			return sprites;
		}

		public abstract List<SpriteDef> defineSprites(BSEntity entity);

		public BindLayerAction<T> layer(Layer layer) {
			this.layer = layer;
			return this;
		}

		public BindLayerAction<T> offset(FPVector offset) {
			this.offset = Optional.of(offset);
			return this;
		}

		public BindLayerAction<T> offset(Optional<FPVector> offset) {
			this.offset = offset;
			return this;
		}
	}

	public static abstract class BindRotateAction<T> extends BindLayerAction<T> {
		public static class BindRotateNoAction<T> extends BindRotateAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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
		public static class BindRotateDirFrameNoAction<T> extends BindRotateDirFrameAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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
		public static class BindRotateFrameNoAction<T> extends BindRotateFrameAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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
		public static class BindVarNoAction<T> extends BindVarAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

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

	public static abstract class BindVarFrameAction<T> extends BindFrameAction<T> {
		public static class BindVarFrameNoAction<T> extends BindVarFrameAction<T> {
			@Override
			public List<SpriteDef> defineSprites(BSEntity entity) {
				return ImmutableList.of();
			}
		}

		protected int variation = 0;

		@Override
		public BindVarFrameAction<T> layer(Layer layer) {
			super.layer(layer);
			return this;
		}

		public BindVarFrameAction<T> variation(int variation) {
			this.variation = variation;
			return this;
		}
	}

	private List<BindAction<?>> bindings;
	private Optional<List<FPCircuitConnectorDefinition>> circuitConnectors = Optional.empty();
	private List<FPFluidBox> fluidBoxes;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		for (BindAction<?> bindAction : bindings) {
			List<LayeredSpriteDef> sprites = bindAction.defineLayeredSprites(entity);
			for (SpritesWithLayer spritesWithLayer : sprites) {
				register.accept(RenderUtils.spriteRenderer(spritesWithLayer.getLayer(), spritesWithLayer.getSprites(),
						entity, drawBounds));
			}
		}

		Direction dir = entity.direction;
		for (FPFluidBox fluidBox : fluidBoxes) {
			if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
				for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
					if (!conn.connectionType.equals("normal")) {
						continue;
					}
					Direction facing = conn.direction.get().rotate(dir);

					Point2D.Double pos;
					if (conn.position.isPresent()) {
						pos = dir.rotate(conn.position.get().createPoint());
					} else {
						pos = conn.positions.get().get(dir.cardinal()).createPoint();
					}

					if (fluidBox.pipePicture.isPresent()) {
						List<Sprite> sprites = fluidBox.pipePicture.get().defineSprites(facing);
						for (Sprite sprite : sprites) {
							sprite.bounds.x += pos.x;
							sprite.bounds.y += pos.y;
						}
						register.accept(RenderUtils.spriteRenderer(sprites, entity, drawBounds));
					}

					if (fluidBox.pipeCovers.isPresent() && map.isPipe(facing.offset(pos, 1.0), facing)) {
						List<Sprite> sprites = fluidBox.pipeCovers.get().defineSprites(facing);
						for (Sprite sprite : sprites) {
							sprite.bounds.x += pos.x;
							sprite.bounds.y += pos.y;
						}
						register.accept(RenderUtils.spriteRenderer(sprites, entity, drawBounds));
					}
				}
			}
		}
	}

	public abstract void defineEntity(Bindings bind, LuaTable lua);

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {
		if (circuitConnectors.isPresent()) {
			List<FPWireConnectionPoint> points = circuitConnectors.get().stream().flatMap(cc -> cc.points.stream())
					.collect(Collectors.toList());
			if (points.size() > 0) {
				consumer.accept(1, WirePoints.fromWireConnectionPoints(points, WireColor.RED, false));
				consumer.accept(2, WirePoints.fromWireConnectionPoints(points, WireColor.GREEN, false));
			}
		}
	}

	@Override
	public void initFromPrototype() {
		List<BindAction<?>> bindings = new ArrayList<>();
		fluidBoxes = new ArrayList<>();
		Bindings fluent = new Bindings(bindings);
		defineEntity(fluent, prototype.lua());
		this.bindings = bindings;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public double initWireConnector(Consumer<MapRenderable> register, MapEntity entity, List<MapEntity> wired) {

		if (circuitConnectors.isPresent()) {
			FPCircuitConnectorDefinition circuitConnector = RenderUtils.pickDirectional(circuitConnectors.get(),
					entity);

			circuitConnector.sprites.ifPresent(sprites -> {
				if (sprites.connectorMain.isPresent()) {
					RenderUtils.createSprites(register, sprites.connectorMain.get().defineSprites(), entity);
					//TODO
					asdgasgd
					register.accept(RenderUtils.spriteRenderer(,
							drawBounds));
				}
				if (sprites.connectorShadow.isPresent()) {
					register.accept(RenderUtils.spriteRenderer(sprites.connectorShadow.get().defineSprites(), entity,
							drawBounds));
				}
				if (sprites.wirePins.isPresent()) {
					register.accept(
							RenderUtils.spriteRenderer(sprites.wirePins.get().defineSprites(), entity, drawBounds));
				}
				if (sprites.wirePinsShadow.isPresent()) {
					register.accept(RenderUtils.spriteRenderer(sprites.wirePinsShadow.get().defineSprites(), entity,
							drawBounds));
				}
			});
		}

		return entity.directionRaw / 16.0;
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		Direction dir = entity.direction;
		for (FPFluidBox fluidBox : fluidBoxes) {
			for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
				if (conn.direction.isPresent() && conn.position.isPresent()) {
					Direction facing = conn.direction.get().rotate(dir);
					Point2D.Double pos = dir.rotate(conn.position.get().createPoint());
					pos.x += entity.position.x;
					pos.y += entity.position.y;
					// TODO use flow direction for pipe arrow logistics
					map.setPipe(pos, facing);
				}
			}
		}
	}

}
