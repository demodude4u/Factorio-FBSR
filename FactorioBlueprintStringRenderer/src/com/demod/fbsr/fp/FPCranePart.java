package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;

public class FPCranePart {
    public final float orientationShift;
    public final boolean isContractibleByCropping;
    public final boolean shouldScaleForPerspective;
    public final boolean scaleToFitModel;
    public final boolean allowSpriteRotation;
    public final float snapStart;
    public final float snapEnd;
    public final float snapEndArmExtentMultiplier;

    public final FPVector3D relativePosition;
    public final FPVector3D relativePositionGrappler;
    public final FPVector3D staticLength;
    public final FPVector3D extendableLength;
    public final FPVector3D staticLengthGrappler;
    public final FPVector3D extendableLengthGrappler;

    public final Optional<FPSprite> sprite;
    public final Optional<FPRotatedSprite> rotatedSprite;
    public final Optional<FPSprite> spriteShadow;
    public final Optional<FPRotatedSprite> rotatedSpriteShadow;

    public final String name;
    public final Layer layer;

    private boolean grappler;

    public FPCranePart(ModsProfile profile, LuaValue lua) {
        
        orientationShift = lua.get("orientation_shift").optfloat(0);
        isContractibleByCropping = lua.get("is_contractible_by_cropping").optboolean(false);
        shouldScaleForPerspective = lua.get("should_scale_for_perspective").optboolean(true);
        scaleToFitModel = lua.get("scale_to_fit_model").optboolean(false);
        allowSpriteRotation = lua.get("allow_sprite_rotation").optboolean(true);
        snapStart = lua.get("snap_start").optfloat(0);
        snapEnd = lua.get("snap_end").optfloat(0);
        snapEndArmExtentMultiplier = lua.get("snap_end_arm_extent_multiplier").optfloat(0);
        
        relativePosition = FPUtils.opt(lua.get("relative_position"), FPVector3D::new).orElse(new FPVector3D(0, 0, 0));
        relativePositionGrappler = FPUtils.opt(lua.get("relative_position_grappler"), FPVector3D::new).orElse(new FPVector3D(0, 0, 0));;
        staticLength = FPUtils.opt(lua.get("static_length"), FPVector3D::new).orElse(new FPVector3D(0, 0, 0));;
        extendableLength = FPUtils.opt(lua.get("extendable_length"), FPVector3D::new).orElse(new FPVector3D(0, 0, 0));;
        staticLengthGrappler = FPUtils.opt(lua.get("static_length_grappler"), FPVector3D::new).orElse(new FPVector3D(0, 0, 0));;
        extendableLengthGrappler = FPUtils.opt(lua.get("extendable_length_grappler"), FPVector3D::new).orElse(new FPVector3D(0, 0, 0));;
        
        sprite = FPUtils.opt(profile, lua.get("sprite"), FPSprite::new);
        rotatedSprite = FPUtils.opt(profile, lua.get("rotated_sprite"), FPRotatedSprite::new);
        spriteShadow = FPUtils.opt(profile, lua.get("sprite_shadow"), FPSprite::new);
        rotatedSpriteShadow = FPUtils.opt(profile, lua.get("rotated_sprite_shadow"), FPRotatedSprite::new);

        name = lua.get("name").optjstring("???");
        int layerRaw = lua.get("layer").optint(0);
        switch (layerRaw) {
            case -3: layer = Layer.ELEVATED_HIGHER_OBJECT; break;
            case -2: layer = Layer.CRANE_GRAPPLER_LOWER; break;
            case -1: layer = Layer.CRANE_GRAPPLER; break;
            case 0: layer = Layer.CRANE_ARM; break;
            case 1: layer = Layer.CRANE_JOINT; break;
            case 2: layer = Layer.CRANE_JOINT_HIGHER; break;
            default: throw new IllegalArgumentException("Invalid layer value: " + layerRaw);
        }

        grappler = name.equals("grappler-claw") || name.equals("telescope");
    }

    public boolean isGrappler() {
        return grappler;
    }

    public void getDefs(Consumer<ImageDef> register, double orientation) {
        sprite.ifPresent(s -> s.defineSprites(register));
        spriteShadow.ifPresent(s -> s.defineSprites(register));
        rotatedSprite.ifPresent(s -> s.defineSprites(register, orientation));
        rotatedSpriteShadow.ifPresent(s -> s.defineSprites(register, orientation));
    }
}
