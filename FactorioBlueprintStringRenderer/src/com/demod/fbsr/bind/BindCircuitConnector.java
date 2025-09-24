package com.demod.fbsr.bind;

import java.util.ArrayList;
import java.util.List;

import com.demod.fbsr.fp.FPCircuitConnectorDefinition;

public class BindCircuitConnector extends BindConditional {
    public static final BindCircuitConnector NOOP = new BindCircuitConnector(null);

    private final List<FPCircuitConnectorDefinition> definitions;

    public BindCircuitConnector(List<FPCircuitConnectorDefinition> definitions) {
        this.definitions = definitions;
    }

    public List<FPCircuitConnectorDefinition> getDefinitions() {
        return definitions;
    }
}
