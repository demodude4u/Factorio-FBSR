package com.demod.fbsr.bind;

import java.util.List;
import java.util.Optional;

import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.map.MapEntity;

public class BindFluidBox extends BindConditional {
    public static final BindFluidBox NOOP = new BindFluidBox(null);

    private final List<FPFluidBox> fluidBoxes;

    @FunctionalInterface
    public interface BindConnectorConditionalTest {
        boolean check(WorldMap worldMap, MapEntity entity, FPFluidBox fluidBox, FPPipeConnectionDefinition connection);
    }
    private Optional<BindConnectorConditionalTest> connectorConditional = Optional.empty();

    private boolean ignorePipeCovers = false;

    public BindFluidBox(List<FPFluidBox> fluidBoxes) {
        this.fluidBoxes = fluidBoxes;
    }

    public List<FPFluidBox> getFluidBoxes() {
        return fluidBoxes;
    }

    public BindFluidBox connectorConditional(BindConnectorConditionalTest test) {
        this.connectorConditional = Optional.of(test);
        return this;
    }

    public BindFluidBox ignorePipeCovers() {
        ignorePipeCovers = true;
        return this;
    }

    public boolean showPipeCovers() {
        return !ignorePipeCovers;
    }

    public boolean connectorTest(WorldMap worldMap, MapEntity entity, FPFluidBox fluidBox, FPPipeConnectionDefinition connection) {
        return connectorConditional.map(c -> c.check(worldMap, entity, fluidBox, connection)).orElse(true);
    }
}
