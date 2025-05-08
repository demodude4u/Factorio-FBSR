package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSCraftingMachineEntity extends BSEntity {
	public final Optional<String> recipe;
	public final Optional<String> recipeQuality;
	public final boolean mirror;

	private Optional<RecipePrototype> protoRecipe = Optional.empty();
	private boolean fluidInput = false;
	private boolean fluidOutput = false;

	public BSCraftingMachineEntity(JSONObject json) {
		super(json);

		recipe = BSUtils.optString(json, "recipe");
		recipeQuality = BSUtils.optString(json, "recipe_quality");
		mirror = json.optBoolean("mirror");
	}

	public BSCraftingMachineEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		recipe = BSUtils.optString(legacy.json(), "recipe");
		recipeQuality = Optional.empty();
		mirror = false;
	}

	public boolean isFluidInput() {
		return fluidInput;
	}

	public boolean isFluidOutput() {
		return fluidOutput;
	}

	public void setFluidInput(boolean fluidInput) {
		this.fluidInput = fluidInput;
	}

	public void setFluidOutput(boolean fluidOutput) {
		this.fluidOutput = fluidOutput;
	}

    public void setProtoRecipe(Optional<RecipePrototype> flatMap) {
        this.protoRecipe = flatMap;
    }

	public Optional<RecipePrototype> getProtoRecipe() {
		return protoRecipe;
	}
}