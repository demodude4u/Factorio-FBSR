package com.demod.fbsr.bs.control;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSCircuitCondition;
import com.demod.fbsr.bs.BSDeciderCombinationConditions;
import com.demod.fbsr.bs.BSDisplayPanelParameter;
import com.demod.fbsr.bs.BSSignalID;

public class BSTransportBeltControlBehavior {
	public static final int CIRCUIT_HAND_READ_MODE_PULSE = 0;
	public static final int CIRCUIT_HAND_READ_MODE_HOLD = 1;
	public static final int CIRCUIT_HAND_READ_MODE_ENTIRE_BELT_HOLD = 2;

	public final Optional<Boolean> circuitEnabled;
	public final Optional<BSCircuitCondition> circuitCondition;
	public final Optional<Boolean> connectToLogisticNetwork;
	public final Optional<BSCircuitCondition> logisticCondition;

	public final Optional<Boolean> circuitReadHandContents;
	public final OptionalInt circuitHandReadMode;
	public final OptionalInt circuitContentsReadMode;
	public final Optional<Boolean> circuitReadResources;

	public final Optional<Boolean> setRecipe;
	public final Optional<Boolean> readContents;
	public final Optional<Boolean> readIngredients;
	public final Optional<Boolean> circuitSetStackSize;
	public final Optional<Boolean> circuitConditionEnabled;
	public final OptionalInt circuitModeOfOperation;

	public final Optional<Boolean> readFromTrain;
	public final Optional<BSSignalID> trainStoppedSignal;
	public final Optional<Boolean> sendToTrain;

	public final Optional<Boolean> readRobotStats;
	public final Optional<BSDeciderCombinationConditions> deciderConditions;
	public final Optional<BSDisplayPanelParameter> parameters;

	public final Optional<Boolean> useColors;
	public final OptionalInt colorMode;
	public final Optional<BSSignalID> redSignal;
	public final Optional<BSSignalID> greenSignal;
	public final Optional<BSSignalID> blueSignal;

	public final Optional<String> operation;
	public final OptionalInt randomUpdateInterval;

	public BSTransportBeltControlBehavior(JSONObject json) {

		// TODO most of these are not used in this specific control behavior, they need
		// to be defined in their own control behavior classes

		circuitEnabled = BSUtils.optBool(json, "circuit_enabled");
		circuitCondition = BSUtils.opt(json, "circuit_condition", BSCircuitCondition::new);
		connectToLogisticNetwork = BSUtils.optBool(json, "connect_to_logistic_network");
		logisticCondition = BSUtils.opt(json, "logistic_condition", BSCircuitCondition::new);

		circuitReadHandContents = BSUtils.optBool(json, "circuit_read_hand_contents");
		circuitHandReadMode = BSUtils.optInt(json, "circuit_hand_read_mode");
		circuitContentsReadMode = BSUtils.optInt(json, "circuit_contents_read_mode");
		circuitReadResources = BSUtils.optBool(json, "circuit_read_resources");

		setRecipe = BSUtils.optBool(json, "set_recipe");
		readContents = BSUtils.optBool(json, "read_contents");
		readIngredients = BSUtils.optBool(json, "read_ingredients");
		circuitSetStackSize = BSUtils.optBool(json, "circuit_set_stack_size");
		circuitConditionEnabled = BSUtils.optBool(json, "circuit_condition_enabled");
		circuitModeOfOperation = BSUtils.optInt(json, "circuit_mode_of_operation");

		readFromTrain = BSUtils.optBool(json, "read_from_train");
		trainStoppedSignal = BSUtils.opt(json, "train_stopped_signal", BSSignalID::new);
		sendToTrain = BSUtils.optBool(json, "send_to_train");

		readRobotStats = BSUtils.optBool(json, "read_robot_stats");
		deciderConditions = BSUtils.opt(json, "decider_conditions", BSDeciderCombinationConditions::new);
		parameters = BSUtils.opt(json, "parameters", BSDisplayPanelParameter::new);

		useColors = BSUtils.optBool(json, "use_colors");
		colorMode = BSUtils.optInt(json, "color_mode");
		redSignal = BSUtils.opt(json, "red_signal", BSSignalID::new);
		greenSignal = BSUtils.opt(json, "green_signal", BSSignalID::new);
		blueSignal = BSUtils.opt(json, "blue_signal", BSSignalID::new);

		operation = BSUtils.optString(json, "operation");
		randomUpdateInterval = BSUtils.optInt(json, "random_update_interval");
	}
}
