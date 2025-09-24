package com.demod.fbsr.bind;

import java.util.List;

import com.demod.fbsr.fp.FPFluidBox;

public class BindFluidBox extends BindConditional {
    public static final BindFluidBox NOOP = new BindFluidBox(null);

    private final List<FPFluidBox> fluidBoxes;

    public BindFluidBox(List<FPFluidBox> fluidBoxes) {
        this.fluidBoxes = fluidBoxes;
    }

    public List<FPFluidBox> getFluidBoxes() {
        return fluidBoxes;
    }
}
