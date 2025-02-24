package com.demod.fbsr.fp;

import java.awt.Rectangle;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.map.MapRect;

public class FPAnimationParameters extends FPSpriteParameters {

	protected final int frameCount;
	public final int lineLength;

	public FPAnimationParameters(LuaValue lua) {
		super(lua);

		frameCount = lua.get("frame_count").optint(1);
		int lineLength = lua.get("line_length").optint(0);
		if (lineLength == 0) {
			lineLength = frameCount;
		}
		this.lineLength = lineLength;
	}

	protected SpriteDef defineSprite(int frame) {

		int x = this.x + width * (frame % lineLength);
		int y = this.y + height * (frame / lineLength);

		Rectangle source = new Rectangle(x, y, width, height);
		MapRect bounds = RenderUtils.boundsBySizeShiftScale(width, height, shift.x, shift.y, scale);
		return new SpriteDef(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), source, bounds);
	}

	public int getFrameCount() {
		return frameCount;
	}
}
