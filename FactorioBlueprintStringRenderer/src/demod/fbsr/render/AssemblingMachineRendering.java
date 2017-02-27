package demod.fbsr.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import demod.fbsr.BlueprintEntity;
import demod.fbsr.BlueprintEntity.Direction;
import demod.fbsr.DataPrototype;
import demod.fbsr.DataTable;
import demod.fbsr.FBSRMain;
import demod.fbsr.Utils;
import demod.fbsr.WorldMap;
import demod.fbsr.render.Renderer.Layer;

public class AssemblingMachineRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		super.createRenderers(register, map, dataTable, entity, prototype);

		Sprite sprite = new Sprite();

		LuaValue recipe = entity.lua().get("recipe");
		if (!recipe.isnil()) {
			DataPrototype protoRecipe = dataTable.getRecipes().get(recipe.toString());
			if (protoRecipe.lua().get("icon") != LuaValue.NIL) {
				sprite.image = FBSRMain.getModImage(protoRecipe.lua().get("icon"));
			} else {
				String name;
				if (protoRecipe.lua().get("results") != LuaValue.NIL) {
					name = protoRecipe.lua().get("results").get(1).get("name").toString();
				} else {
					name = protoRecipe.lua().get("result").toString();
				}
				DataPrototype protoProduct = dataTable.getItems().get(name);
				if (protoProduct == null) {
					protoProduct = dataTable.getFluids().get(name);
				}
				sprite.image = FBSRMain.getModImage(protoProduct.lua().get("icon"));
			}

			sprite.source = new Rectangle(0, 0, sprite.image.getWidth(), sprite.image.getHeight());
			sprite.bounds = new Rectangle2D.Double(-0.7, -1.0, 1.4, 1.4);

			Renderer delegate = spriteRenderer(sprite, entity, prototype);
			register.accept(new Renderer(Layer.OVERLAY2, delegate.getBounds()) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(new Color(0, 0, 0, 180));
					g.fill(sprite.bounds);
					delegate.render(g);
				}
			});
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		LuaValue recipe = entity.lua().get("recipe");
		boolean hasFluid = false;
		if (!recipe.isnil()) {
			DataPrototype protoRecipe = dataTable.getRecipes().get(recipe.toString());

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
