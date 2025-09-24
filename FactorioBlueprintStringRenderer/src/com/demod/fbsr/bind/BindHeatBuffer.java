package com.demod.fbsr.bind;

import com.demod.fbsr.fp.FPHeatBuffer;

public class BindHeatBuffer extends BindConditional {
    public static final BindHeatBuffer NOOP = new BindHeatBuffer(null);

    private final FPHeatBuffer heatBuffer;

    public BindHeatBuffer(FPHeatBuffer heatBuffer) {
        this.heatBuffer = heatBuffer;
    }

    public FPHeatBuffer getHeatBuffer() {
        return heatBuffer;
    }
}
