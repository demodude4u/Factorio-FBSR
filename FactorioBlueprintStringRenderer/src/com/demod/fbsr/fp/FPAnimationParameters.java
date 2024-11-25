package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

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

	protected Sprite createSprite(int frame) {

		int x = this.x + width * (frame % lineLength);
		int y = this.y + height * (frame / lineLength);

		return RenderUtils.createSprite(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y, width,
				height, shift.x, shift.y, scale);
	}

	public int getFrameCount() {
		return frameCount;
	}
}
