package com.demod.fbsr.entity;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSCraftingMachineEntity;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.google.common.collect.ImmutableList;

public abstract class CraftingMachineRendering extends SimpleEntityRendering {
	private static final int FRAME = 0;

	private FPWorkingVisualisations protoGraphicsSet;
	private Optional<FPWorkingVisualisations> protoGraphicsSetFlipped;
	private List<FPFluidBox> protoConditionalFluidBoxes;

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

		// TODO need a better approach that doesn't involve searching recipe lua
		Optional<String> recipe = bsEntity.recipe;
		boolean hasFluidInput = false;
		boolean hasFluidOutput = false;
		if (recipe.isPresent()) {
			Optional<RecipePrototype> optRecipe = FactorioManager.lookupRecipeByName(recipe.get());
			if (optRecipe.isPresent()) {
				RecipePrototype protoRecipe = optRecipe.get();

				// XXX could be done better
				List<LuaValue> inputs = new ArrayList<>();
				List<LuaValue> outputs = new ArrayList<>();
				Utils.forEach(protoRecipe.lua().get("ingredients").checktable(), (Consumer<LuaValue>) inputs::add);
				LuaValue resultsLua = protoRecipe.lua().get("results");
				if (resultsLua != LuaValue.NIL) {
					outputs.add(resultsLua);
				}
				hasFluidInput = inputs.stream().anyMatch(lua -> {
					LuaValue typeLua = lua.get("type");
					return typeLua != LuaValue.NIL && typeLua.toString().equals("fluid");
				});
				hasFluidOutput = outputs.stream().anyMatch(lua -> {
					LuaValue typeLua = lua.get("type");
					return typeLua != LuaValue.NIL && typeLua.toString().equals("fluid");
				});
			}
		}

		// TODO preload everything
		Direction dir = entity.getDirection();
		for (FPFluidBox fluidBox : protoConditionalFluidBoxes) {
			if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
				for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
					boolean visible = (hasFluidInput && conn.isInput()) || (hasFluidOutput && conn.isOutput());
					if (!visible) {
						continue;
					}

					Direction facing = conn.direction.get().rotate(dir);
					MapPosition offset = dir.rotate(MapPosition.convert(conn.position.get()));
					offset = facing.offset(offset, 1.0);
					if (bsEntity.mirror) {
						offset = offset.multiplyUnit(-1, 0);
					}

					MapPosition point = offset.add(entity.getPosition());
					Consumer<SpriteDef> pointRegister = s -> register.accept(new MapSprite(s, Layer.OBJECT, point));

					if (fluidBox.pipePicture.isPresent()) {
						fluidBox.pipePicture.get().defineSprites(pointRegister, facing);
					}

					if (fluidBox.pipeCovers.isPresent() && !map.isPipe(offset, facing)) {
						fluidBox.pipeCovers.get().defineSprites(pointRegister, facing);
					}
				}
			}
		}

		// TODO need a better approach that doesn't involve searching recipe lua
		if (recipe.isPresent() && map.isAltMode()) {
			Optional<BufferedImage> icon = TagManager.lookup("recipe", recipe.get());
			if (icon.isEmpty()) {
				Optional<RecipePrototype> optRecipe = FactorioManager.lookupRecipeByName(recipe.get());
				if (optRecipe.isPresent()) {
					RecipePrototype protoRecipe = optRecipe.get();
					String name;
					if (protoRecipe.lua().get("results") != LuaValue.NIL) {
						name = protoRecipe.lua().get("results").get(1).get("name").toString();
					} else {
						name = protoRecipe.lua().get("result").toString();
					}
					icon = TagManager.lookup("item", name);
					if (icon.isEmpty()) {
						icon = TagManager.lookup("fluid", name);
					}
				}
			}
			if (icon.isPresent()) {
				register.accept(new MapIcon(entity.getPosition().addUnit(0, -0.3), icon.get(), 1.4, 0.1, false));
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		boolean fluidBoxesOffWhenNoFluidRecipe = lua.get("fluid_boxes_off_when_no_fluid_recipe").optboolean(false);
		if (!fluidBoxesOffWhenNoFluidRecipe) {
			bind.fluidBoxes(lua.get("fluid_boxes"));
			protoConditionalFluidBoxes = ImmutableList.of();
		} else {
			protoConditionalFluidBoxes = FPUtils.list(lua.get("fluid_boxes"), FPFluidBox::new);
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(prototype.lua().get("graphics_set"));
		protoGraphicsSetFlipped = FPUtils.opt(prototype.lua().get("graphics_set_flipped"),
				FPWorkingVisualisations::new);
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		Optional<String> recipe = entity.<BSCraftingMachineEntity>fromBlueprint().recipe;
		if (recipe.isPresent()) {
			Optional<RecipePrototype> optRecipe = data.getTable().getRecipe(recipe.get());
			if (optRecipe.isPresent()) {
				RecipePrototype protoRecipe = optRecipe.get();
				setLogisticMachine(map, entity.getBounds(), protoRecipe);
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		if (!protoConditionalFluidBoxes.isEmpty()) {
			Optional<String> recipe = entity.<BSCraftingMachineEntity>fromBlueprint().recipe;
			boolean hasFluid = false;
			if (recipe.isPresent()) {
				Optional<RecipePrototype> optRecipe = data.getTable().getRecipe(recipe.get());
				if (optRecipe.isPresent()) {
					RecipePrototype protoRecipe = optRecipe.get();

					List<LuaValue> items = new ArrayList<>();
					Utils.forEach(protoRecipe.lua().get("ingredients").checktable(), (Consumer<LuaValue>) items::add);
					LuaValue resultsLua = protoRecipe.lua().get("results");
					if (resultsLua != LuaValue.NIL) {
						items.add(resultsLua);
					}
					hasFluid = items.stream().anyMatch(lua -> {
						LuaValue typeLua = lua.get("type");
						return typeLua != LuaValue.NIL && typeLua.toString().equals("fluid");
					});
				}
			}

			if (hasFluid) {
				Direction dir = entity.getDirection();
				for (FPFluidBox fluidBox : protoConditionalFluidBoxes) {
					for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
						if (conn.direction.isPresent() && conn.position.isPresent()) {
							Direction facing = conn.direction.get().rotate(dir);
							MapPosition pos = dir.rotate(MapPosition.convert(conn.position.get()))
									.add(entity.getPosition());
							map.setPipe(pos, facing);
						}
					}
				}
			}
		}
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSCraftingMachineEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoGraphicsSetFlipped.get().getDefs(register, FRAME);
		protoGraphicsSet.getDefs(register, FRAME);
	}

}
