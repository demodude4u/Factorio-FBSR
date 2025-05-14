package com.demod.fbsr.fp;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.entity.AgriculturalTowerRendering.ShamefulHardcoding;

public class FPAgriculturalCraneProperties {

    public final float minArmExtent;
    public final float minGraplerLength;
    public final float operationAngle;
    public final float telescopeDefaultExtention;
    public final FPVector3D origin;
    public final FPVector3D shadowDirection;
    public final List<FPCranePart> parts;

    public FPAgriculturalCraneProperties(LuaValue lua) {
        minArmExtent = lua.get("min_arm_extent").optfloat(0f);
        minGraplerLength = lua.get("min_grapler_extent").optfloat(0.2f);
        operationAngle = lua.get("operation_angle").optfloat(45) * ((float)(Math.PI / 4f) / 45f);
        telescopeDefaultExtention = lua.get("telescope_default_extention").optfloat(0.5f);
        origin = new FPVector3D(lua.get("origin"));
        shadowDirection = new FPVector3D(lua.get("shadow_direction"));
        parts = FPUtils.list(lua.get("parts"), FPCranePart::new);
    }

    public void getDefs(Consumer<ImageDef> register, Map<String, ShamefulHardcoding> data) {
        for (FPCranePart part : parts) {
            ShamefulHardcoding shc = data.get(part.name);
            if (shc == null) {
                continue;
            }
            part.getDefs(register, shc.spriteOrientation);
        }
    }
}
