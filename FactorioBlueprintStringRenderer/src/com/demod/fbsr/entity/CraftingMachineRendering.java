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
import com.demod.fbsr.bind.Bindings;
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

	protected boolean isFluidBoxesOffWhenNoFluidRecipe() {
		return false;
	}

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

		if (bsEntity.getProtoRecipe().isPresent() && map.isAltMode()) {
			RecipePrototype protoRecipe = bsEntity.getProtoRecipe().get();
			ModdingResolver resolver = entity.getResolver();
			Optional<IconDef> icon = resolver.resolveIconRecipeName(protoRecipe.getName());
			if (icon.isEmpty()) {
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

		bind.fluidBoxes(lua.get("fluid_boxes")).connectorConditional((map, entity, fb, conn) -> {
			BSCraftingMachineEntity bsEntity = entity.<BSCraftingMachineEntity>fromBlueprint();
			return !isFluidBoxesOffWhenNoFluidRecipe() 
					|| (bsEntity.isFluidInput() && conn.isInput()) 
					|| (bsEntity.isFluidOutput() && conn.isOutput());
		});
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
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(profile, prototype.lua().get("graphics_set"));
		protoGraphicsSetFlipped = FPUtils.opt(profile, prototype.lua().get("graphics_set_flipped"),
				FPWorkingVisualisations::new);
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		BSCraftingMachineEntity bsEntity = entity.<BSCraftingMachineEntity>fromBlueprint();
		bsEntity.setProtoRecipe(bsEntity.recipe.flatMap(n -> entity.getResolver().resolveRecipeName(n)));

		if (bsEntity.getProtoRecipe().isPresent()) {
			RecipePrototype protoRecipe = bsEntity.getProtoRecipe().get();
			setLogisticMachine(map, entity.getBounds(), protoRecipe);

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
	}
}
