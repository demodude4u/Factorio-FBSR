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
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class ArithmeticCombinatorRendering extends CombinatorRendering<BSArithmeticCombinatorEntity> {

	public static class BSArithmeticCombinatorEntity extends BSEntity {
		public final Optional<String> playerDescription;
		public final Optional<BSArithmeticConditions> arithmeticConditions;

		public BSArithmeticCombinatorEntity(JSONObject json) {
			super(json);

			playerDescription = BSUtils.optString(json, "player_description");

			if (json.has("control_behavior")) {
				JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
				arithmeticConditions = BSUtils.opt(jsonControlBehavior, "arithmetic_conditions",
						BSArithmeticConditions::new);
			} else {
				arithmeticConditions = Optional.empty();
			}
		}

		public BSArithmeticCombinatorEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			playerDescription = Optional.empty();

			String operationString = legacy.json().getJSONObject("control_behavior")
					.getJSONObject("arithmetic_conditions").getString("operation");
			arithmeticConditions = Optional.of(new BSArithmeticConditions(operationString));
		}
	}

	public static class BSArithmeticConditions {
		public final Optional<BSSignalID> firstSignal;
		public final Optional<BSSignalID> secondSignal;
		public final OptionalDouble firstConstant;
		public final OptionalDouble secondConstant;
		public final String operation;
		public final Optional<BSSignalID> outputSignal;
		public final Optional<BSNetworkPorts> firstSignalNetworks;
		public final Optional<BSNetworkPorts> secondSignalNetworks;

		public BSArithmeticConditions(JSONObject json) {
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

		public BSArithmeticConditions(String legacyOperationString) {
			firstSignal = Optional.empty();
			secondSignal = Optional.empty();
			firstConstant = OptionalDouble.empty();
			secondConstant = OptionalDouble.empty();
			operation = legacyOperationString;
			outputSignal = Optional.empty();
			firstSignalNetworks = Optional.empty();
			secondSignalNetworks = Optional.empty();
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
		return entity.arithmeticConditions.map(bs -> bs.operation);
	}
}
