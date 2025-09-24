package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.bs.entity.BSCraftingMachineEntity;

@EntityType("assembling-machine")
public class AssemblingMachineRendering extends CraftingMachineRendering {

	private boolean protoFluidBoxesOffWhenNoFluidRecipe;

	public boolean isFluidBoxesOffWhenNoFluidRecipe() {
		return protoFluidBoxesOffWhenNoFluidRecipe;
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		boolean hasFlippedConnector = !lua.get("circuit_connector_flipped").isnil();
		bind.circuitConnector4Way(lua.get("circuit_connector")).conditional((map, entity) -> {
			BSCraftingMachineEntity bsEntity = entity.<BSCraftingMachineEntity>fromBlueprint();
			return !hasFlippedConnector || !bsEntity.mirror;
		});
		bind.circuitConnector4Way(lua.get("circuit_connector_flipped")).conditional((map, entity) -> {
			BSCraftingMachineEntity bsEntity = entity.<BSCraftingMachineEntity>fromBlueprint();
			return bsEntity.mirror;
		});
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoFluidBoxesOffWhenNoFluidRecipe = prototype.lua().get("fluid_boxes_off_when_no_fluid_recipe").optboolean(false);
	}
}
