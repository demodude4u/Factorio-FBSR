package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.CraftingMachineRendering.BSCraftingMachineEntity;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.google.common.collect.ImmutableList;

public abstract class CraftingMachineRendering extends SimpleEntityRendering<BSCraftingMachineEntity> {

	public static class BSCraftingMachineEntity extends BSEntity {
		public final Optional<String> recipe;

		public BSCraftingMachineEntity(JSONObject json) {
			super(json);

			recipe = BSUtils.optString(json, "recipe");
		}
	}

	private FPWorkingVisualisations protoGraphicsSet;
	private List<FPFluidBox> protoConditionalFluidBoxes;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BSCraftingMachineEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		register.accept(RenderUtils.spriteRenderer(protoGraphicsSet.createSprites(entity.direction, 0), entity,
				protoSelectionBox));

		Sprite spriteIcon = new Sprite();

		Optional<String> recipe = entity.recipe;
		if (recipe.isPresent() && map.isAltMode()) {
			Optional<RecipePrototype> optRecipe = dataTable.getRecipe(recipe.get());
			if (optRecipe.isPresent()) {
				RecipePrototype protoRecipe = optRecipe.get();
				if (!protoRecipe.lua().get("icon").isnil() || !protoRecipe.lua().get("icons").isnil()) {
					spriteIcon.image = FactorioData.getIcon(protoRecipe);
				} else {
					String name;
					if (protoRecipe.lua().get("results") != LuaValue.NIL) {
						name = protoRecipe.lua().get("results").get(1).get("name").toString();
					} else {
						name = protoRecipe.lua().get("result").toString();
					}
					Optional<? extends DataPrototype> protoProduct = dataTable.getItem(name);
					if (!protoProduct.isPresent()) {
						protoProduct = dataTable.getFluid(name);
					}
					spriteIcon.image = protoProduct.map(FactorioData::getIcon).orElse(RenderUtils.EMPTY_IMAGE);
				}

				spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
				spriteIcon.bounds = new Rectangle2D.Double(-0.7, -1.0, 1.4, 1.4);

				Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, protoSelectionBox);
				register.accept(new Renderer(Layer.ENTITY_INFO_ICON, delegate.getBounds()) {
					@Override
					public void render(Graphics2D g) throws Exception {
						g.setColor(new Color(0, 0, 0, 180));
						g.fill(spriteIcon.bounds);
						delegate.render(g);
					}
				});
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		boolean fluidBoxesOffWhenNoFluidRecipe = lua.get("fluid_boxes_off_when_no_fluid_recipe").optboolean(false);
		if (!fluidBoxesOffWhenNoFluidRecipe) {
			bind.fluidBoxes(lua.get("fluid_boxes"));
			protoConditionalFluidBoxes = ImmutableList.of();
		} else {
			protoConditionalFluidBoxes = FPUtils.list(lua.get("fluid_boxes"), FPFluidBox::new);
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoGraphicsSet = new FPWorkingVisualisations(prototype.lua().get("graphics_set"));
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSCraftingMachineEntity entity) {
		Optional<String> recipe = entity.recipe;
		if (recipe.isPresent()) {
			Optional<RecipePrototype> optRecipe = dataTable.getRecipe(recipe.get());
			if (optRecipe.isPresent()) {
				RecipePrototype protoRecipe = optRecipe.get();
				setLogisticMachine(map, dataTable, entity, protoRecipe);
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSCraftingMachineEntity entity) {
		super.populateWorldMap(map, dataTable, entity);

		if (!protoConditionalFluidBoxes.isEmpty()) {

			Optional<String> recipe = entity.recipe;
			boolean hasFluid = false;
			if (recipe.isPresent()) {
				Optional<RecipePrototype> optRecipe = dataTable.getRecipe(recipe.get());
				if (optRecipe.isPresent()) {
					RecipePrototype protoRecipe = optRecipe.get();

					List<LuaValue> items = new ArrayList<>();
					Utils.forEach(protoRecipe.lua().get("ingredients"), (Consumer<LuaValue>) items::add);
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
				Direction dir = entity.direction;
				for (FPFluidBox fluidBox : protoConditionalFluidBoxes) {
					for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
						if (conn.direction.isPresent() && conn.position.isPresent()) {
							Direction facing = conn.direction.get().rotate(dir);
							Point2D.Double pos = dir.rotatePoint(conn.position.get().createPoint());
							pos.x += entity.position.x;
							pos.y += entity.position.y;
							map.setPipe(pos, facing);
						}
					}
				}
			}
		}
	}

}
