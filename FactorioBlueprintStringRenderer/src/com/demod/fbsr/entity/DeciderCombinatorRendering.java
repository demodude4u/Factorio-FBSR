package com.demod.fbsr.entity;

import java.util.Map;
import java.util.Optional;

import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSDeciderCombinatorEntity;
import com.demod.fbsr.map.MapEntity;

public class DeciderCombinatorRendering extends CombinatorRendering {

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("=", "equal_symbol_sprites");
		operations.put(">", "greater_symbol_sprites");
		operations.put("<", "less_symbol_sprites");
		operations.put("\u2260", "not_equal_symbol_sprites");
		operations.put("\u2264", "less_or_equal_symbol_sprites");
		operations.put("\u2265", "greater_or_equal_symbol_sprites");

	}

	@Override
	public Optional<String> getOperation(MapEntity entity) {
		return entity.<BSDeciderCombinatorEntity>fromBlueprint().deciderConditions
				.flatMap(bs -> bs.conditions.stream().findFirst()).map(bs -> bs.comparator);
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSDeciderCombinatorEntity.class;
	}
}
