package com.demod.fbsr.bind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.entity.EntityRendering;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.fp.FPAnimationVariations;
import com.demod.fbsr.fp.FPCircuitConnectorDefinition;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPHeatBuffer;
import com.demod.fbsr.fp.FPLayeredSprite;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.fp.FPRotatedAnimation8Way;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.fp.FPWorkingVisualisation;
import com.demod.fbsr.map.MapEntity;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class Bindings {
	private final Profile profile;
	
	private List<BindDef> defs = new ArrayList<>();
	private List<BindCircuitConnector> circuitConnectors = new ArrayList<>();
	private List<BindFluidBox> fluidBoxes = new ArrayList<>();
	private List<BindHeatBuffer> heatBuffers = new ArrayList<>();

	public Bindings(Profile profile) {
		this.profile = profile;
	}

	public List<BindDef> getDefs() {
		return defs;
	}

	public List<BindCircuitConnector> getCircuitConnectors() {
		return circuitConnectors;
	}

	public List<BindFluidBox> getFluidBoxes() {
		return fluidBoxes;
	}

	public List<BindHeatBuffer> getHeatBuffers() {
		return heatBuffers;
	}

	private <T extends BindDef, P> T addDefs(LuaValue lua, BiFunction<Profile, LuaValue, P> protoCtor, Function<P, T> bindCtor, Supplier<T> bindNoopCtor) {
		return addDefs(lua, l -> protoCtor.apply(profile, l), bindCtor, bindNoopCtor);
	}

	private <T extends BindDef, P> T addDefs(LuaValue lua, Function<LuaValue, P> protoCtor, Function<P, T> bindCtor, Supplier<T> bindNoopCtor) {
		if (lua.isnil()) {
			return bindNoopCtor.get();
		}
		P proto = protoCtor.apply(lua);
		return addDefs(proto, () -> bindCtor.apply(proto));
	}

	private <T extends BindDef, P> T addDefs(P proto, Supplier<T> bindCtor) {
		T ret = bindCtor.get();
		defs.add(ret);
		return ret;
	}

	public BindFrameDef animation(LuaValue lua) {
		return addDefs(lua, FPAnimation::new, proto -> new BindFrameDef() {
			@Override
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
				proto.defineSprites(consumer, frame);
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
				proto.defineSprites(register, frame);
			}
		}, BindFrameDef::new);
	}

	public BindDirFrameDef animation4Way(LuaValue lua) {
		return addDefs(lua, FPAnimation4Way::new, proto -> new BindDirFrameDef() {
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
		}, BindDirFrameDef::new);
	}

	public BindVarFrameDef animationVariations(LuaValue lua) {
		return addDefs(lua, FPAnimationVariations::new, proto -> new BindVarFrameDef() {
			@Override
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
				proto.defineSprites(consumer, variation, frame);
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
				proto.defineSprites(register, variation, frame);
			}
		}, BindVarFrameDef::new);
	}

	public BindCircuitConnector circuitConnector(LuaValue lua) {
		if (lua.isnil()) {
			return BindCircuitConnector.NOOP;
		}
		BindCircuitConnector bind = new BindCircuitConnector(
				ImmutableList.of(new FPCircuitConnectorDefinition(profile, lua)));
		circuitConnectors.add(bind);
		return bind;
	}

	public BindCircuitConnector circuitConnector16Way(LuaValue lua) {
		if (lua.isnil()) {
			return BindCircuitConnector.NOOP;
		}
		List<FPCircuitConnectorDefinition> list = FPUtils.list(profile, lua, FPCircuitConnectorDefinition::new);
		Preconditions.checkArgument(list.size() == 16,
				"Expected 16 circuit connectors, but found " + list.size());
		BindCircuitConnector bind = new BindCircuitConnector(list);
		circuitConnectors.add(bind);
		return bind;
	}

	public BindCircuitConnector circuitConnector4Way(LuaValue lua) {
		if (lua.isnil()) {
			return BindCircuitConnector.NOOP;
		}
		List<FPCircuitConnectorDefinition> list = FPUtils.list(profile, lua, FPCircuitConnectorDefinition::new);
		Preconditions.checkArgument(list.size() == 4,
				"Expected 4 circuit connectors, but found " + list.size());
		BindCircuitConnector bind = new BindCircuitConnector(list);
		circuitConnectors.add(bind);
		return bind;
	}

	public BindCircuitConnector circuitConnector8Way(LuaValue lua) {
		if (lua.isnil()) {
			return BindCircuitConnector.NOOP;
		}
		List<FPCircuitConnectorDefinition> list = FPUtils.list(profile, lua, FPCircuitConnectorDefinition::new);
		Preconditions.checkArgument(list.size() == 8,
				"Expected 8 circuit connectors, but found " + list.size());
		BindCircuitConnector bind = new BindCircuitConnector(list);
		circuitConnectors.add(bind);
		return bind;
	}

	public BindCircuitConnector circuitConnectorNWay(LuaValue lua) {
		if (lua.isnil()) {
			return BindCircuitConnector.NOOP;
		}
		List<FPCircuitConnectorDefinition> list = FPUtils.list(profile, lua, FPCircuitConnectorDefinition::new);
		BindCircuitConnector bind = new BindCircuitConnector(list);
		circuitConnectors.add(bind);
		return bind;
	}

	public BindFluidBox fluidBox(LuaValue lua) {
		if (lua.isnil()) {
			return BindFluidBox.NOOP;
		}
		return fluidBox(new FPFluidBox(profile, lua));
	}

	public BindFluidBox fluidBox(FPFluidBox proto) {
		BindFluidBox bind = new BindFluidBox(
				ImmutableList.of(proto));
		fluidBoxes.add(bind);
		return bind;
	}

	public BindFluidBox fluidBoxes(LuaValue lua) {
		if (lua.isnil()) {
			return BindFluidBox.NOOP;
		}
		List<FPFluidBox> list = FPUtils.list(profile, lua, FPFluidBox::new);
		BindFluidBox bind = new BindFluidBox(list);
		fluidBoxes.add(bind);
		return bind;
	}

	public BindHeatBuffer heatBuffer(LuaValue lua) {
		if (lua.isnil()) {
			return BindHeatBuffer.NOOP;
		}
		BindHeatBuffer bind = new BindHeatBuffer(new FPHeatBuffer(profile, lua));
		heatBuffers.add(bind);
		return bind;
	}

	public BindConditional energySource(LuaValue lua) {
		if (lua.isnil()) {
			return BindConditional.NOOP;
		}
		LuaValue luaType = lua.get("type");
		switch (luaType.checkjstring()) {
			case "electric":
			case "burner":
			case "heat":
				return heatBuffer(lua);
			case "void":
			default:
				return BindConditional.NOOP;
			case "fluid":
				return fluidBox(lua.get("fluid_box"));
		}
	}

	public BindDef layeredSprite(LuaValue lua) {
		return addDefs(lua, FPLayeredSprite::new, proto -> new BindDef() {
			@Override
			public void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity) {
				proto.defineLayeredSprites(consumer);
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
				proto.defineLayeredSprites(register);
			}
		}, BindDef::new);
	}

	public BindRotateFrameDef rotatedAnimation(LuaValue lua) {
		return addDefs(lua, FPRotatedAnimation::new, proto -> new BindRotateFrameDef() {
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
		}, BindRotateFrameDef::new);
	}

	public BindRotateFrameDef rotatedAnimationLimited(LuaValue lua,
			int limitDirectionCount) {
		return addDefs(lua, (p, l) -> new FPRotatedAnimation(p, l, limitDirectionCount), proto -> new BindRotateFrameDef() {
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
		}, BindRotateFrameDef::new);
	}

	public BindRotateDirFrameDef rotatedAnimation8Way(LuaValue lua) {
		return addDefs(lua, FPRotatedAnimation8Way::new, proto -> new BindRotateDirFrameDef() {
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
		}, BindRotateDirFrameDef::new);
	}

	public BindRotateDef rotatedSprite(LuaValue lua) {
		return addDefs(lua, FPRotatedSprite::new, proto -> new BindRotateDef() {
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
		}, BindRotateDef::new);
	}

	public BindRotateDef rotatedSpriteLimited(LuaValue lua, int limitDirectionCount) {
		return addDefs(lua, (p, l) -> new FPRotatedSprite(p, l, limitDirectionCount), proto -> new BindRotateDef() {
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
		}, BindRotateDef::new);
	}

	public BindLayerDef sprite(LuaValue lua) {
		return addDefs(lua, FPSprite::new, proto -> new BindLayerDef() {
			@Override
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
				proto.defineSprites(consumer);
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
				proto.defineSprites(register);
			}
		}, BindLayerDef::new);
	}

	public BindDirDef sprite4Way(LuaValue lua) {
		if (lua.isnil()) {
			return new BindDirDef();
		}
		return sprite4Way(new FPSprite4Way(profile, lua));
	}

	public BindDirDef sprite4Way(FPSprite4Way proto) {
		return addDefs(proto, () -> new BindDirDef() {
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
		});
	}

	public BindVarDef spriteVariations(LuaValue lua) {
		return addDefs(lua, FPSpriteVariations::new, proto -> new BindVarDef() {
			@Override
			public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {
				proto.defineSprites(consumer, variation);
			}

			@Override
			public void initAtlas(Consumer<ImageDef> register) {
				proto.defineSprites(register, variation);
			}
		}, BindVarDef::new);
	}

	public BindDirFrameDef workingVisualisation(FPWorkingVisualisation proto) {
		return addDefs(proto, () -> new BindDirFrameDef() {
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
		});
	}
}