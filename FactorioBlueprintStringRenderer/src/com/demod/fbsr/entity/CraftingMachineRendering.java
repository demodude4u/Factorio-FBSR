package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSCraftingMachineEntity;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.google.common.collect.ImmutableList;

public abstract class CraftingMachineRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	private FPWorkingVisualisations protoGraphicsSet;
	private Optional<FPWorkingVisualisations> protoGraphicsSetFlipped;
	private boolean protoFluidBoxesOffWhenNoFluidRecipe;
	private List<FPFluidBox> protoFluidBoxes;


	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);

		BSCraftingMachineEntity bsEntity = entity.<BSCraftingMachineEntity>fromBlueprint();

		if (bsEntity.mirror && protoGraphicsSetFlipped.isPresent()) {
			protoGraphicsSetFlipped.get().defineSprites(entityRegister, entity.getDirection(), FRAME);
		} else {
			protoGraphicsSet.defineSprites(entityRegister, entity.getDirection(), FRAME);
		}

		Direction dir = entity.getDirection();
		for (FPFluidBox fluidBox : protoFluidBoxes) {
			if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
				for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
					boolean visible = !protoFluidBoxesOffWhenNoFluidRecipe 
							|| (bsEntity.isFluidInput() && conn.isInput()) 
							|| (bsEntity.isFluidOutput() && conn.isOutput());
					if (!visible) {
						continue;
					}
					if (conn.direction.isEmpty() || conn.position.isEmpty()) {
						continue;
					}

					Direction connDir = conn.direction.get();
					MapPosition connPos = MapPosition.convert(conn.position.get());
					
					if (bsEntity.mirror) {
						connDir = connDir.flipX();
						connPos = connPos.flipX();
					}
					
					Direction facing = connDir.rotate(dir);
					MapPosition point = facing.offset(dir.rotate(connPos).add(entity.getPosition()), 1);
					Consumer<SpriteDef> pointRegister = s -> register.accept(new MapSprite(s, Layer.OBJECT, point));

					if (fluidBox.pipePicture.isPresent()) {
						fluidBox.pipePicture.get().defineSprites(pointRegister, facing);
					}

					if (fluidBox.pipeCovers.isPresent() && !map.isPipe(point, facing)) {
						fluidBox.pipeCovers.get().defineSprites(pointRegister, facing);
					}
				}
			}
		}

		if (bsEntity.recipe.isPresent() && map.isAltMode()) {

			ModdingResolver resolver = entity.getResolver();

			Optional<IconDef> icon = resolver.resolveIconRecipeName(bsEntity.recipe.get());
			if (icon.isEmpty()) {
				if (bsEntity.getProtoRecipe().isPresent()) {
					RecipePrototype protoRecipe = bsEntity.getProtoRecipe().get();
					String name;
					if (protoRecipe.lua().get("results") != LuaValue.NIL) {
						name = protoRecipe.lua().get("results").get(1).get("name").toString();
					} else {
						name = protoRecipe.lua().get("result").toString();
					}
					icon = resolver.resolveIconItemName(name);
					if (icon.isEmpty()) {
						icon = resolver.resolveIconFluidName(name);
					}
				}
			}
			if (icon.isPresent()) {
				register.accept(new MapIcon(entity.getPosition().addUnit(0, -0.3), icon.get(), 1.4,
						OptionalDouble.of(0.1), false, bsEntity.recipeQuality.filter(s -> !s.equals("normal")), resolver));
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.energySource(lua.get("energy_source"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSCraftingMachineEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		if (protoGraphicsSetFlipped.isPresent()) {
			protoGraphicsSetFlipped.get().getDefs(register, FRAME);
		}
		protoGraphicsSet.getDefs(register, FRAME);
		protoFluidBoxes.forEach(fp -> fp.getDefs(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(profile, prototype.lua().get("graphics_set"));
		protoGraphicsSetFlipped = FPUtils.opt(profile, prototype.lua().get("graphics_set_flipped"),
				FPWorkingVisualisations::new);

		protoFluidBoxesOffWhenNoFluidRecipe = prototype.lua().get("fluid_boxes_off_when_no_fluid_recipe").optboolean(false);
		protoFluidBoxes = FPUtils.list(profile, prototype.lua().get("fluid_boxes"), FPFluidBox::new);
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		Optional<String> recipe = entity.<BSCraftingMachineEntity>fromBlueprint().recipe;
		if (recipe.isPresent()) {
			Optional<RecipePrototype> optRecipe = profile.getFactorioData().getTable().getRecipe(recipe.get());
			if (optRecipe.isPresent()) {
				RecipePrototype protoRecipe = optRecipe.get();
				setLogisticMachine(map, entity.getBounds(), protoRecipe);
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		BSCraftingMachineEntity bsEntity = entity.<BSCraftingMachineEntity>fromBlueprint();

		bsEntity.setProtoRecipe(bsEntity.recipe.flatMap(n -> entity.getResolver().resolveRecipeName(n)));

		if (bsEntity.getProtoRecipe().isPresent()) {
			RecipePrototype protoRecipe = bsEntity.getProtoRecipe().get();

			List<LuaValue> inputs = new ArrayList<>();
			List<LuaValue> outputs = new ArrayList<>();
			LuaValue luaIngredients = protoRecipe.lua().get("ingredients");
			if (!luaIngredients.isnil()) {
				Utils.forEach(luaIngredients.checktable(), (Consumer<LuaValue>) inputs::add);
			}
			LuaValue resultsLua = protoRecipe.lua().get("results");
			if (!resultsLua.isnil()) {
				outputs.add(resultsLua);
			}
			boolean fluidInput = inputs.stream().anyMatch(lua -> {
				LuaValue typeLua = lua.get("type");
				return typeLua != LuaValue.NIL && typeLua.toString().equals("fluid");
			});
			boolean fluidOutput = outputs.stream().anyMatch(lua -> {
				LuaValue typeLua = lua.get("type");
				return typeLua != LuaValue.NIL && typeLua.toString().equals("fluid");
			});

			bsEntity.setFluidInput(fluidInput);
			bsEntity.setFluidOutput(fluidOutput);
		}

		Direction dir = entity.getDirection();
		for (FPFluidBox fluidBox : protoFluidBoxes) {
			for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
				boolean visible = !protoFluidBoxesOffWhenNoFluidRecipe 
						|| (bsEntity.isFluidInput() && conn.isInput()) 
						|| (bsEntity.isFluidOutput() && conn.isOutput());
				if (!visible) {
					continue;
				}

				if (conn.direction.isPresent() && conn.position.isPresent()) {
					Direction connDir = conn.direction.get();
					MapPosition connPos = MapPosition.convert(conn.position.get());
					
					if (bsEntity.mirror) {
						connDir = connDir.flipX();
						connPos = connPos.flipX();
					}

					Direction facing = connDir.rotate(dir);
					MapPosition pos = dir.rotate(connPos).add(entity.getPosition());
					map.setPipe(pos, facing);
				}
			}
		}
	}

}
