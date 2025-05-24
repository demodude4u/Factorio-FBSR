package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;

public class FPFenceDirectionSet {
    public final Optional<FPSpriteVariations> north;
    public final Optional<FPSpriteVariations> northeast;
    public final Optional<FPSpriteVariations> east;
    public final Optional<FPSpriteVariations> southeast;
    public final Optional<FPSpriteVariations> south;
    public final Optional<FPSpriteVariations> southwest;
    public final Optional<FPSpriteVariations> west;
    public final Optional<FPSpriteVariations> northwest;

    public FPFenceDirectionSet(ModsProfile profile, LuaValue lua) {
        north = FPUtils.opt(profile, lua.get("north"), FPSpriteVariations::new);
        northeast = FPUtils.opt(profile, lua.get("northeast"), FPSpriteVariations::new);
        east = FPUtils.opt(profile, lua.get("east"), FPSpriteVariations::new);
        southeast = FPUtils.opt(profile, lua.get("southeast"), FPSpriteVariations::new);
        south = FPUtils.opt(profile, lua.get("south"), FPSpriteVariations::new);
        southwest = FPUtils.opt(profile, lua.get("southwest"), FPSpriteVariations::new);
        west = FPUtils.opt(profile, lua.get("west"), FPSpriteVariations::new);
        northwest = FPUtils.opt(profile, lua.get("northwest"), FPSpriteVariations::new);
    }

    public void getDefs(Consumer<ImageDef> register) {
        north.ifPresent(fp -> fp.getDefs(register));
        northeast.ifPresent(fp -> fp.getDefs(register));
        east.ifPresent(fp -> fp.getDefs(register));
        southeast.ifPresent(fp -> fp.getDefs(register));
        south.ifPresent(fp -> fp.getDefs(register));
        southwest.ifPresent(fp -> fp.getDefs(register)); 
        west.ifPresent(fp -> fp.getDefs(register));
        northwest.ifPresent(fp -> fp.getDefs(register));
    }

    public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction, int variation) {
        switch (direction) {
            case NORTH:
                north.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case NORTHEAST:
                northeast.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case EAST:
                east.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case SOUTHEAST:
                southeast.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case SOUTH:
                south.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case SOUTHWEST:
                southwest.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case WEST:
                west.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
            case NORTHWEST:
                northwest.ifPresent(fp -> fp.defineSprites(consumer, variation));
                break;
        }
    }

    public List<SpriteDef> defineSprites(Direction direction, int variation) {
        List<SpriteDef> ret = new ArrayList<>();
        defineSprites(ret::add, direction, variation);
        return ret;
    }
}
