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

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class AssemblingMachineRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		LuaValue animationLua = prototype.lua().get("idle_animation");
		if (animationLua.isnil()) {
			animationLua = prototype.lua().get("animation");
		}
		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(animationLua, entity.getDirection());

		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));

		Sprite spriteIcon = new Sprite();

		String recipe = entity.json().optString("recipe", null);
		if (recipe != null) {
			Optional<RecipePrototype> optRecipe = dataTable.getRecipe(recipe);
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

				Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, prototype);
				register.accept(new Renderer(Layer.OVERLAY2, delegate.getBounds()) {
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
	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		String recipeName = entity.json().optString("recipe", null);
		if (recipeName != null) {
			Optional<RecipePrototype> optRecipe = dataTable.getRecipe(recipeName);
			if (optRecipe.isPresent()) {
				RecipePrototype protoRecipe = optRecipe.get();
				setLogisticMachine(map, dataTable, entity, prototype, protoRecipe);
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		String recipeName = entity.json().optString("recipe", null);
		boolean hasFluid = false;
		if (recipeName != null) {
			Optional<RecipePrototype> optRecipe = dataTable.getRecipe(recipeName);
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

		LuaValue fluidBoxesLua = prototype.lua().get("fluid_boxes");
		boolean offWhenNoFluidRecipe = fluidBoxesLua.isnil() ? true
				: fluidBoxesLua.get("off_when_no_fluid_recipe").optboolean(false);

		if (!fluidBoxesLua.isnil() && (!offWhenNoFluidRecipe || hasFluid)) {
			Utils.forEach(fluidBoxesLua, fluidBoxLua -> {
				if (!fluidBoxLua.istable()) {
					return;
				}
				Utils.forEach(fluidBoxLua.get("pipe_connections"), pipeConnectionLua -> {
					Point2D.Double offset = Utils.parsePoint2D(pipeConnectionLua.get("position"));
					if (Math.abs(offset.y) > Math.abs(offset.x)) {
						offset.y += -Math.signum(offset.y);
					} else {
						offset.x += -Math.signum(offset.x);
					}
					Point2D.Double pos = entity.getDirection().left()
							.offset(entity.getDirection().back().offset(entity.getPosition(), offset.y), offset.x);
					Direction direction = offset.y > 0 ? entity.getDirection().back() : entity.getDirection();
					map.setPipe(pos, direction);
				});
			});
		}
	}

}
