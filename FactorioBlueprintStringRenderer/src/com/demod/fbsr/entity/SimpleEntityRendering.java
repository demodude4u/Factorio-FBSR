package com.demod.fbsr.entity;

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
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
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
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

//TODO needs to be renamed not to be confused with SimpleEntityPrototype...
public abstract class SimpleEntityRendering extends EntityRendererFactory {

	public static abstract class BindAction<T> {
		public static class BindNoAction<T> extends BindAction<T> {
			@Override
			public void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
			}
		}

		protected T proto;

		public abstract void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity);

		public abstract void initAtlas(Consumer<ImageDef> register);
	}

	public static abstract class BindDirAction<T> extends BindLayerAction<T> {
		public static class BindDirNoAction<T> extends BindDirAction<T> {
			@Override
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					proto.defineSprites(consumer, frame);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					proto.defineSprites(register, frame);
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					proto.defineSprites(consumer, direction.orElse(entity.getDirection()), frame);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (direction.isPresent()) {
						proto.defineSprites(register, direction.get(), frame);
					} else {
						proto.getDefs(register, frame);
					}
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					proto.defineSprites(consumer, variation, frame);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					proto.defineSprites(register, variation, frame);
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
				public void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity) {
					proto.defineLayeredSprites(consumer);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					proto.defineLayeredSprites(register);
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.fromBlueprint().orientation.isPresent()) {
						orientation = entity.fromBlueprint().orientation.getAsDouble();
					} else {
						orientation = entity.fromBlueprint().directionRaw / 16.0;
					}
					proto.defineSprites(consumer, orientation, frame);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (orientation.isPresent()) {
						proto.defineSprites(register, orientation.getAsDouble(), frame);
					} else {
						proto.getDefs(register, frame);
					}
				}
			};
			ret.proto = new FPRotatedAnimation(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateFrameAction<FPRotatedAnimation> rotatedAnimationLimited(LuaValue lua,
				int limitDirectionCount) {
			if (lua.isnil()) {
				return new BindRotateFrameNoAction<>();
			}
			BindRotateFrameAction<FPRotatedAnimation> ret = new BindRotateFrameAction<FPRotatedAnimation>() {
				@Override
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.fromBlueprint().orientation.isPresent()) {
						orientation = entity.fromBlueprint().orientation.getAsDouble();
					} else {
						orientation = entity.fromBlueprint().directionRaw / 16.0;
					}
					proto.defineSprites(consumer, orientation, frame);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (orientation.isPresent()) {
						proto.defineSprites(register, orientation.getAsDouble(), frame);
					} else {
						proto.getDefs(register, frame);
					}
				}
			};
			ret.proto = new FPRotatedAnimation(lua, limitDirectionCount);
			bindings.add(ret);
			return ret;
		}

		public BindRotateDirFrameAction<FPRotatedAnimation8Way> rotatedAnimation8Way(LuaValue lua) {
			if (lua.isnil()) {
				return new BindRotateDirFrameNoAction<>();
			}
			BindRotateDirFrameAction<FPRotatedAnimation8Way> ret = new BindRotateDirFrameAction<FPRotatedAnimation8Way>() {
				@Override
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.fromBlueprint().orientation.isPresent()) {
						orientation = entity.fromBlueprint().orientation.getAsDouble();
					} else {
						orientation = entity.getDirection().getOrientation();
					}
					proto.defineSprites(consumer, direction.orElse(entity.getDirection()), orientation, frame);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (direction.isPresent() && orientation.isPresent()) {
						proto.defineSprites(register, direction.get(), orientation.getAsDouble(), frame);
					} else if (direction.isPresent()) {
						proto.getDefs(register, direction.get(), frame);
					} else if (orientation.isPresent()) {
						proto.getDefs(register, orientation.getAsDouble(), frame);
					} else {
						proto.getDefs(register, frame);
					}
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.fromBlueprint().orientation.isPresent()) {
						orientation = entity.fromBlueprint().orientation.getAsDouble();
					} else {
						orientation = entity.getDirection().getOrientation();
					}
					proto.defineSprites(consumer, orientation);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (orientation.isPresent()) {
						proto.defineSprites(register, orientation.getAsDouble());
					} else {
						proto.getDefs(register);
					}
				}
			};
			ret.proto = new FPRotatedSprite(lua);
			bindings.add(ret);
			return ret;
		}

		public BindRotateAction<FPRotatedSprite> rotatedSpriteLimited(LuaValue lua, int limitDirectionCount) {
			if (lua.isnil()) {
				return new BindRotateNoAction<>();
			}
			BindRotateAction<FPRotatedSprite> ret = new BindRotateAction<FPRotatedSprite>() {
				@Override
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					double orientation;
					if (this.orientation.isPresent()) {
						orientation = this.orientation.getAsDouble();
					} else if (entity.fromBlueprint().orientation.isPresent()) {
						orientation = entity.fromBlueprint().orientation.getAsDouble();
					} else {
						orientation = entity.getDirection().getOrientation();
					}
					proto.defineSprites(consumer, orientation);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (orientation.isPresent()) {
						proto.defineSprites(register, orientation.getAsDouble());
					} else {
						proto.getDefs(register);
					}
				}
			};
			ret.proto = new FPRotatedSprite(lua, limitDirectionCount);
			bindings.add(ret);
			return ret;
		}

		public BindLayerAction<FPSprite> sprite(LuaValue lua) {
			if (lua.isnil()) {
				return new BindLayerNoAction<>();
			}
			BindLayerAction<FPSprite> ret = new BindLayerAction<FPSprite>() {
				@Override
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					proto.defineSprites(consumer);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					proto.defineSprites(register);
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					proto.defineSprites(consumer, direction.orElse(entity.getDirection()));
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					if (direction.isPresent()) {
						proto.defineSprites(register, direction.get());
					} else {
						proto.getDefs(register);
					}
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
				public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
					proto.defineSprites(consumer, variation);
				}

				@Override
				public void initAtlas(Consumer<ImageDef> register) {
					proto.defineSprites(register, variation);
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
			}
		}

		protected Layer layer = Layer.OBJECT;
		protected Optional<MapPosition> offset = Optional.empty();

		@Override
		public void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity) {
			// XXX I don't like these transient instances
			defineSprites(s -> {
				LayeredSpriteDef sprite = new LayeredSpriteDef(s, layer);
				if (offset.isPresent()) {
					sprite.offset(offset.get());
				}
				consumer.accept(sprite);
			}, entity);
		}

		public abstract void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity);

		public BindLayerAction<T> layer(Layer layer) {
			this.layer = layer;
			return this;
		}

		public BindLayerAction<T> offset(MapPosition offset) {
			this.offset = Optional.of(offset);
			return this;
		}

		public BindLayerAction<T> offset(Optional<MapPosition> offset) {
			this.offset = offset;
			return this;
		}
	}

	public static abstract class BindRotateAction<T> extends BindLayerAction<T> {
		public static class BindRotateNoAction<T> extends BindRotateAction<T> {
			@Override
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
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
		Consumer<LayeredSpriteDef> entityRegister = entity.spriteRegister(register);

		for (BindAction<?> bindAction : bindings) {
			bindAction.defineLayeredSprites(entityRegister, entity);
		}

		Direction dir = entity.getDirection();
		for (FPFluidBox fluidBox : fluidBoxes) {
			if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
				for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
					if (!conn.connectionType.equals("normal")) {
						continue;
					}
					Direction facing = conn.direction.get().rotate(dir);

					MapPosition offset;
					if (conn.position.isPresent()) {
						offset = dir.rotate(MapPosition.convert(conn.position.get()));
					} else {
						offset = MapPosition.convert(conn.positions.get().get(dir.cardinal()));
					}
					MapPosition pos = entity.getPosition().add(offset);

					if (fluidBox.pipePicture.isPresent()) {
						fluidBox.pipePicture.get()
								.defineSprites(s -> register.accept(new MapSprite(s, Layer.OBJECT, pos)), facing);
					}

					if (fluidBox.pipeCovers.isPresent() && map.isPipe(facing.offset(pos, 1.0), facing)) {
						fluidBox.pipeCovers.get()
								.defineSprites(s -> register.accept(new MapSprite(s, Layer.OBJECT, pos)), facing);
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
	public void initAtlas(Consumer<ImageDef> register) {

		for (BindAction<?> bindAction : bindings) {
			bindAction.initAtlas(register);
		}

		for (FPFluidBox fluidBox : fluidBoxes) {
			if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
				for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
					if (!conn.connectionType.equals("normal")) {
						continue;
					}
					fluidBox.pipePicture.ifPresent(fp -> fp.getDefs(register));
					fluidBox.pipeCovers.ifPresent(fp -> fp.getDefs(register));
				}
			}
		}

		if (circuitConnectors.isPresent()) {
			for (FPCircuitConnectorDefinition circuitConnector : circuitConnectors.get()) {
				circuitConnector.sprites.ifPresent(sprites -> {
					sprites.connectorMain.ifPresent(fp -> fp.defineSprites(register));
					sprites.connectorShadow.ifPresent(fp -> fp.defineSprites(register));
					sprites.wirePins.ifPresent(fp -> fp.defineSprites(register));
					sprites.wirePinsShadow.ifPresent(fp -> fp.defineSprites(register));
				});
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

	@Override
	public double initWireConnector(Consumer<MapRenderable> register, MapEntity entity, List<MapEntity> wired) {

		if (circuitConnectors.isPresent() && !wired.isEmpty()) {
			FPCircuitConnectorDefinition circuitConnector = RenderUtils.pickDirectional(circuitConnectors.get(),
					entity);

			Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);

			circuitConnector.sprites.ifPresent(sprites -> {
				sprites.connectorMain.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.connectorShadow.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.wirePins.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.wirePinsShadow.ifPresent(fp -> fp.defineSprites(entityRegister));
			});
		}

		return entity.fromBlueprint().directionRaw / 16.0;
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		for (FPFluidBox fluidBox : fluidBoxes) {
			for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
				if (conn.direction.isPresent() && conn.position.isPresent()) {
					Direction facing = conn.direction.get().rotate(dir);
					MapPosition pos = dir.rotate(MapPosition.convert(conn.position.get())).add(entity.getPosition());
					// TODO use flow direction for pipe arrow logistics
					map.setPipe(pos, facing);
				}
			}
		}
	}

}
