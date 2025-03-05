package com.demod.fbsr.entity;

import java.util.Map;
import java.util.Optional;

import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSSelectorCombinatorEntity;
import com.demod.fbsr.map.MapEntity;

public class SelectorCombinatorRendering extends CombinatorRendering {

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("select", "min_symbol_sprites");// TODO change to max_symbol_sprites if selectMax true
		operations.put("count", "count_symbol_sprites");
		operations.put("random", "random_symbol_sprites");
		operations.put("stack-size", "stack_size_sprites");
		operations.put("rocket-capacity", "rocket_capacity_sprites");
		operations.put("quality-filter", "quality_symbol_sprites");
		operations.put("quality-transfer", "quality_symbol_sprites");
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSSelectorCombinatorEntity.class;
	}

	@Override
	public Optional<String> getOperation(MapEntity entity) {
		return entity.<BSSelectorCombinatorEntity>fromBlueprint().operation;
	}
}
