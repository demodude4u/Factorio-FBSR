package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.IconDefWithQuality;
import com.demod.fbsr.IconManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSNetworkPorts;
import com.demod.fbsr.bs.BSSignalID;
import com.demod.fbsr.bs.entity.BSArithmeticCombinatorEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("arithmetic-combinator")
public class ArithmeticCombinatorRendering extends CombinatorRendering {

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
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();
		BSArithmeticCombinatorEntity bsEntity = entity.fromBlueprint();

		if (bsEntity.arithmeticConditions.isPresent()) {

			IconManager iconManager = profile.getIconManager();

			List<IconDefWithQuality> inputIcons = new ArrayList<>();
			bsEntity.arithmeticConditions.get().firstSignal
					.ifPresent(s -> iconManager.lookupSignalID(s.type, s.name, s.quality).ifPresent(inputIcons::add));
			bsEntity.arithmeticConditions.get().secondSignal
					.ifPresent(s -> iconManager.lookupSignalID(s.type, s.name, s.quality).ifPresent(inputIcons::add));

			List<IconDefWithQuality> outputIcons = new ArrayList<>();
			bsEntity.arithmeticConditions.get().outputSignal
					.ifPresent(s -> iconManager.lookupSignalID(s.type, s.name, s.quality).ifPresent(outputIcons::add));

			double iconStartY = entity.getDirection().isHorizontal() ? -0.5 : -0.25;

			List<List<IconDefWithQuality>> iconRows = Arrays.asList(inputIcons, outputIcons);
			for (int row = 0; row < iconRows.size(); row++) {
				List<IconDefWithQuality> icons = iconRows.get(row);
				if (!icons.isEmpty()) {
					MapPosition rowPos = pos.addUnit(-(icons.size() - 1) * 0.25, iconStartY + row * 0.5);
					for (int i = 0; i < icons.size(); i++) {
						IconDefWithQuality icon = icons.get(i);
						MapPosition iconPos = rowPos.addUnit(i * 0.5, 0);
						register.accept(icon.createMapIcon(iconPos, 0.4, OptionalDouble.of(0.05), false));
					}
				}
			}
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
	public Class<? extends BSEntity> getEntityClass() {
		return BSArithmeticCombinatorEntity.class;
	}

	@Override
	public Optional<String> getOperation(MapEntity entity) {
		return entity.<BSArithmeticCombinatorEntity>fromBlueprint().arithmeticConditions.map(bs -> bs.operation);
	}
}
