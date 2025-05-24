package com.demod.fbsr.fp;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.SpriteDef;

public class FPSlopedSprite extends FPRotatedSprite {
    public final double slopeAngleBetweenFrames;
    public final boolean slopeBackEqualsFront;

    public FPSlopedSprite(ModsProfile profile, LuaValue lua, double slopeAngleBetweenFrames, boolean slopeBackEqualsFront) {
        super(profile, lua, (p, l) -> new FPSlopedSprite(p, l, slopeAngleBetweenFrames, slopeBackEqualsFront));

        this.slopeAngleBetweenFrames = slopeAngleBetweenFrames;
        this.slopeBackEqualsFront = slopeBackEqualsFront;
    }

    public void defineSprites(Consumer<SpriteDef> consumer, Direction dir, double slope) {
        if (layers.isPresent()) {
            for (FPRotatedSprite layer : layers.get()) {
                ((FPSlopedSprite)layer).defineSprites(consumer, dir, slope);
            }
            return;
        }

        int index = getIndex(dir, slope);
        consumer.accept(defs.get(index));
	}

    private int getIndex(Direction dir, double slope) {
		int framesPerSlope = directionCount / ((slopeBackEqualsFront ? 4 : 8));
		double angleToSlopeSelect = 180.0 / (slopeAngleBetweenFrames * Math.PI);
		int slopeSelect = (int)Math.round(Math.atan(slope / FPUtils.PROJECTION_CONSTANT) * angleToSlopeSelect);
		slopeSelect = Math.max(-framesPerSlope, Math.min(framesPerSlope, slopeSelect));

		if (dir == Direction.NORTH) {
			if (dir.getDx() < 0) {
				slopeSelect = -slopeSelect;
			}
			int slopeIndex = framesPerSlope - slopeSelect;
			if (slopeSelect < 0) {
				slopeIndex--;
			}
			return slopeIndex;
		}

		if (dir == Direction.EAST) {
			if (dir.getDy() < 0) {
				slopeSelect = -slopeSelect;
			}
			int slopeIndex = framesPerSlope - slopeSelect;
			if (slopeSelect < 0) {
				slopeIndex--;
			}
			slopeIndex += 2 * framesPerSlope;
			return slopeIndex;
		}

		if (dir == Direction.SOUTH) {
			if (slopeBackEqualsFront) {
				slopeSelect = -slopeSelect;
			}
			int slopeIndex = framesPerSlope - slopeSelect;
			if (slopeSelect < 0) {
				slopeIndex--;
			}
			if (!slopeBackEqualsFront) {
				slopeIndex += 4 * framesPerSlope;
			}
			return slopeIndex;
		}

		if (dir == Direction.WEST) {
			if (slopeBackEqualsFront) {
				slopeSelect = -slopeSelect;
			}
			int slopeIndex = framesPerSlope - slopeSelect;
			if (slopeSelect < 0) {
				slopeIndex--;
			}
			slopeIndex += 2 * framesPerSlope;
			if (!slopeBackEqualsFront) {
				slopeIndex += 4 * framesPerSlope;
			}
			return slopeIndex;
		}

		throw new IllegalArgumentException("Invalid direction: " + dir);
	}
}
