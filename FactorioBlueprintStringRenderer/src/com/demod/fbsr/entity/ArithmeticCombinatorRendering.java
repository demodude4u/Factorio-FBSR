package com.demod.fbsr.entity;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSNetworkPorts;
import com.demod.fbsr.bs.BSSignalID;
import com.demod.fbsr.entity.ArithmeticCombinatorRendering.BSArithmeticCombinatorEntity;

public class ArithmeticCombinatorRendering extends CombinatorRendering<BSArithmeticCombinatorEntity> {

	public static class BSArithmeticCombinatorEntity extends BSEntity {
		public final Optional<BSArithmeticCondition> conditions;

		public BSArithmeticCombinatorEntity(JSONObject json) {
			super(json);

			if (json.has("control_behavior")) {
				JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
				conditions = BSUtils.opt(jsonControlBehavior, "arithmetic_conditions", BSArithmeticCondition::new);
			} else {
				conditions = Optional.empty();
			}
		}
	}

	public static class BSArithmeticCondition {
		public final Optional<BSSignalID> firstSignal;
		public final Optional<BSSignalID> secondSignal;
		public final OptionalDouble firstConstant;
		public final OptionalDouble secondConstant;
		public final String operation;
		public final Optional<BSSignalID> outputSignal;
		public final Optional<BSNetworkPorts> firstSignalNetworks;
		public final Optional<BSNetworkPorts> secondSignalNetworks;

		public BSArithmeticCondition(JSONObject json) {
			firstSignal = BSUtils.opt(json, "first_signal", BSSignalID::new);
			secondSignal = BSUtils.opt(json, "second_signal", BSSignalID::new);
			// TODO check if this should be int or double
			firstConstant = BSUtils.optDouble(json, "first_constant");
			secondConstant = BSUtils.optDouble(json, "second_constant");
			operation = json.getString("operation");
			outputSignal = BSUtils.opt(json, "output_signal", BSSignalID::new);
			firstSignalNetworks = BSUtils.opt(json, "first_signal_networks", BSNetworkPorts::new);
			secondSignalNetworks = BSUtils.opt(json, "second_signal_networks", BSNetworkPorts::new);
		}
	}

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
	public Optional<String> getOperation(BSArithmeticCombinatorEntity entity) {
		return entity.conditions.map(bs -> bs.operation);
	}

	@Override
	public BSArithmeticCombinatorEntity parseEntity(JSONObject json) throws Exception {
		return new BSArithmeticCombinatorEntity(json);
	}

}
