package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapRenderable;

public class FurnaceRendering extends CraftingMachineRendering {

	private List<String> protoCraftingCategories;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		List<String> outputs = map.getLogisticGridCells(getDrawBounds(entity).asMapRect()).stream()
				.flatMap(c -> c.getOutputs().stream().flatMap(o -> o.stream())).distinct().collect(Collectors.toList());

		if (outputs.size() == 1 && map.isAltMode()) {
			List<RecipePrototype> recipes = prototype.getTable().getRecipesByInput(outputs.get(0)).stream()
					.filter(r -> protoCraftingCategories.contains(r.getCategory())).collect(Collectors.toList());
			if (recipes.size() == 1) {
				RecipePrototype recipe = recipes.get(0);
				Optional<IconDef> icon = IconManager.lookupRecipe(recipe.getName());
				if (icon.isEmpty()) {
					String name;
					if (recipe.lua().get("results") != LuaValue.NIL) {
						name = recipe.lua().get("results").get(1).get("name").toString();
					} else {
						name = recipe.lua().get("result").toString();
					}
					icon = IconManager.lookupItem(name);
					if (icon.isEmpty()) {
						icon = IconManager.lookupFluid(name);
					}
				}
				if (icon.isPresent()) {
					register.accept(new MapIcon(entity.getPosition().addUnit(0, -0.3), icon.get(), 1.4,
							OptionalDouble.of(0.1), false, Optional.empty()));
				}
			}
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoCraftingCategories = FPUtils.list(prototype.lua().get("crafting_categories"), l -> l.tojstring());
	}

}
