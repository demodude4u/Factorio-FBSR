package com.demod.fbsr.fp;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

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

	protected Sprite createSprite(FactorioData data, int frame) {

		int x = this.x + width * (frame % lineLength);
		int y = this.y + height * (frame / lineLength);

		return RenderUtils.createSprite(data, filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y, width,
				height, shift.x, shift.y, scale);
	}

	public int getFrameCount() {
		return frameCount;
	}
}
