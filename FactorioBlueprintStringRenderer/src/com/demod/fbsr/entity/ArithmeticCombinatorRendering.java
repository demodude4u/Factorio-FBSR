package com.demod.fbsr.entity;

import java.util.Map;
import java.util.Optional;

import com.demod.fbsr.bs.BSEntity;

public class ArithmeticCombinatorRendering extends CombinatorRendering {

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("+", "plus_symbol_sprites");
		operations.put("-", "minus_symbol_sprites");
		operations.put("*", "multiply_symbol_sprites");
		operations.put("/", "divide_symbol_sprites");
		operations.put("AND", "and_symbol_sprites");
		operations.put("OR", "or_symbol_sprites");
		operations.put("XOR", "xor_symbol_sprites");
		operations.put("MOD", "modulo_symbol_sprites");
		operations.put("%", "modulo_symbol_sprites");
		operations.put(">>", "right_shift_symbol_sprites");
		operations.put("<<", "left_shift_symbol_sprites");
		operations.put("^", "power_symbol_sprites");
	}

	@Override
	public Optional<String> getOperation(BSEntity entity) {
		return entity.controlBehavior.flatMap(bs -> bs.arithmeticConditions).map(bs -> bs.operation);
	}

}
