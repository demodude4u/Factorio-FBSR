package com.demod.fbsr.entity;

import java.util.Map;
import java.util.Optional;

import com.demod.fbsr.bs.BSEntity;

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
	public Optional<String> getOperation(BSEntity entity) {
		return entity.controlBehavior.flatMap(bs -> bs.deciderConditions)
				.flatMap(bs -> bs.conditions.stream().findFirst()).map(bs -> bs.comparator);
	}
}
