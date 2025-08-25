package com.demod.fbsr.entity.elevatedpipes;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.entity.FurnaceRendering;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

@EntityType(value = "furnace", modded = true)
public class ElevatedPipeRendering extends FurnaceRendering {
    public static final int MAX_DISTANCE = 10;

    private FPSprite protoPipeHorizontalLeft;
    private FPSprite protoPipeHorizontalRight;
    private FPSprite protoPipeHorizontalCenter;
    private FPSprite protoPipeHorizontalSingle;
    private FPSprite protoPipeVerticalTop;
    private FPSprite protoPipeVerticalBottom;
    private FPSprite protoPipeVerticalCenter;
    private FPSprite protoPipeVerticalSingle;

    @Override
    public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
        super.createRenderers(register, map, entity);

        registerPipePiecesIfPresent(register, map, entity, Direction.WEST,
                protoPipeHorizontalRight, 
                protoPipeHorizontalLeft, 
                protoPipeHorizontalCenter, 
                protoPipeHorizontalSingle);
        registerPipePiecesIfPresent(register, map, entity, Direction.NORTH,
                protoPipeVerticalBottom,
                protoPipeVerticalTop,
                protoPipeVerticalCenter,
                protoPipeVerticalSingle);
    }

    private void registerPipePiecesIfPresent(Consumer<MapRenderable> register, WorldMap map, MapEntity entity, 
            Direction dir, FPSprite first, FPSprite last, FPSprite center, FPSprite single) {
        MapPosition pos = entity.getPosition();
        
        OptionalInt dist = OptionalInt.empty();
        for (int distCheck = 2; distCheck <= MAX_DISTANCE; distCheck++) {
            if (map.isElevatedPipe(dir.offset(pos, distCheck))) {
                dist = OptionalInt.of(distCheck);
                break;
            }
        }

        if (dist.isPresent()) {
            int d = dist.getAsInt();
            if (d == 2) {
                single.defineSprites(
                    entity.spriteRegister(register, Layer.UNDER_ELEVATED, dir.offset(1))
                );
            } else {
                for (int i = 1; i < d; i++) {
                    FPSprite protoPipe;
                    if (i == 1) {
                        protoPipe = first;
                    } else if (i == d - 1) {
                        protoPipe = last;
                    } else {
                        protoPipe = center;
                    }
                    protoPipe.defineSprites(
                        entity.spriteRegister(register, Layer.UNDER_ELEVATED, dir.offset(i))
                    );
                }
            }
        }
    }

    @Override
    public void initFromPrototype() {
        super.initFromPrototype();

        LuaValue luaSprite = prototype.getTable().getRaw("sprite").get();

        protoPipeHorizontalLeft = new FPSprite(profile, luaSprite.get("elevated-pipe-horizontal-left"));
        protoPipeHorizontalRight = new FPSprite(profile, luaSprite.get("elevated-pipe-horizontal-right"));
        protoPipeHorizontalCenter = new FPSprite(profile, luaSprite.get("elevated-pipe-horizontal-center"));
        protoPipeHorizontalSingle = new FPSprite(profile, luaSprite.get("elevated-pipe-horizontal-single"));
        protoPipeVerticalTop = new FPSprite(profile, luaSprite.get("elevated-pipe-vertical-top"));
        protoPipeVerticalBottom = new FPSprite(profile, luaSprite.get("elevated-pipe-vertical-bottom"));
        protoPipeVerticalCenter = new FPSprite(profile, luaSprite.get("elevated-pipe-vertical-center"));
        protoPipeVerticalSingle = new FPSprite(profile, luaSprite.get("elevated-pipe-vertical-single"));
    }

    @Override
    public void initAtlas(Consumer<ImageDef> register) {
        super.initAtlas(register);

        protoPipeHorizontalLeft.defineSprites(register);
        protoPipeHorizontalRight.defineSprites(register);
        protoPipeHorizontalCenter.defineSprites(register);
        protoPipeHorizontalSingle.defineSprites(register);
        protoPipeVerticalTop.defineSprites(register);
        protoPipeVerticalBottom.defineSprites(register);
        protoPipeVerticalCenter.defineSprites(register);
        protoPipeVerticalSingle.defineSprites(register);
    }

    @Override
    public void populateWorldMap(WorldMap map, MapEntity entity) {
        super.populateWorldMap(map, entity);

        map.setElevatedPipe(entity.getPosition(), entity);
    }
}
