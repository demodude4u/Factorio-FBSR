package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;

public class FPRailFencePictureSet {
    public final List<FPFenceDirectionSet> ends;
    public final FPFenceDirectionSet fence;
    public final Optional<List<FPFenceDirectionSet>> endsUpper;
    public final Optional<FPFenceDirectionSet> fenceUpper;

    public FPRailFencePictureSet(LuaValue lua) {
        ends = FPUtils.list(lua.get("ends"), FPFenceDirectionSet::new);
        fence = new FPFenceDirectionSet(lua.get("fence"));
        endsUpper = FPUtils.optList(lua.get("ends_upper"), FPFenceDirectionSet::new);
        fenceUpper = FPUtils.opt(lua.get("fence_upper"), FPFenceDirectionSet::new);
    }

    public void getDefs(Consumer<ImageDef> register) {
        ends.forEach(e -> e.getDefs(register));
        fence.getDefs(register);
        endsUpper.ifPresent(l -> l.forEach(e -> e.getDefs(register)));
        fenceUpper.ifPresent(f -> f.getDefs(register));
    }
}
