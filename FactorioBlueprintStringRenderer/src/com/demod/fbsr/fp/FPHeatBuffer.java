package com.demod.fbsr.fp;

import java.util.List;
import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;

public class FPHeatBuffer {

    public final Optional<FPSprite4Way> pipeCovers;
    public final Optional<FPSprite4Way> heatPipeCovers;
    public final Optional<FPSprite4Way> heatPicture;
    public final Optional<FPSprite4Way> heatGlow;
    public final List<FPHeatConnection> connections;

    public FPHeatBuffer(Profile profile, LuaValue lua) {
        pipeCovers = FPUtils.opt(profile, lua.get("pipe_covers"), FPSprite4Way::new);
        heatPipeCovers = FPUtils.opt(profile, lua.get("heat_pipe_covers"), FPSprite4Way::new);
        heatPicture = FPUtils.opt(profile, lua.get("heat_picture"), FPSprite4Way::new);
        heatGlow = FPUtils.opt(profile, lua.get("heat_glow"), FPSprite4Way::new);
        connections = FPUtils.list(lua.get("connections"), FPHeatConnection::new);
    }
}
