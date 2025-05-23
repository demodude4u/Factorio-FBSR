package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.map.MapRect;

public class FPAnimationParameters extends FPSpriteParameters {

	public final int frameCount;
	public final int lineLength;

	private final List<SpriteDef> defs;

	public FPAnimationParameters(ModsProfile profile, LuaValue lua) {
		super(profile, lua);

		frameCount = lua.get("frame_count").optint(1);
		int lineLength = lua.get("line_length").optint(0);
		if (lineLength == 0) {
			lineLength = frameCount;
		}
		this.lineLength = lineLength;

		defs = createDefs(profile);
	}

	private List<SpriteDef> createDefs(ModsProfile profile) {
		List<SpriteDef> defs = new ArrayList<>();
		for (int frame = 0; frame < frameCount; frame++) {
			int x = this.x + width * (frame % lineLength);
			int y = this.y + height * (frame / lineLength);

			Rectangle source = new Rectangle(x, y, width, height);
			double scaledWidth = scale * width / FBSR.TILE_SIZE;
			double scaledHeight = scale * height / FBSR.TILE_SIZE;
			MapRect bounds = MapRect.byUnit(shift.x - scaledWidth / 2.0, shift.y - scaledHeight / 2.0, scaledWidth,
					scaledHeight);
			if (filename.isPresent()) {
				defs.add(new SpriteDef(profile, filename.get(), drawAsShadow, blendMode, tint.map(FPColor::createColor),
						tintAsOverlay, applyRuntimeTint, source, bounds));
			}
		}
		return defs;
	}

	protected SpriteDef defineSprite(int frame) {
		return defs.get(frame);
	}

	public int getFrameCount() {
		return frameCount;
	}
}
