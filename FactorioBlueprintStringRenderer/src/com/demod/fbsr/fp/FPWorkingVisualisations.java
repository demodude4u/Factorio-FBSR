package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;

public class FPWorkingVisualisations {
	public final Optional<FPAnimation4Way> animation;
	public final Optional<FPAnimation4Way> idleAnimation;
	public final Optional<List<FPWorkingVisualisation>> workingVisualisations;

	public FPWorkingVisualisations(LuaValue lua) {
		idleAnimation = FPUtils.opt(lua.get("idle_animation"), FPAnimation4Way::new);
		animation = FPUtils.opt(lua.get("animation"), FPAnimation4Way::new).or(() -> idleAnimation);
		workingVisualisations = FPUtils.optList(lua.get("working_visualisations"), FPWorkingVisualisation::new);
	}

	public void createSprites(Consumer<Sprite> consumer, Direction direction, int frame) {
		animation.ifPresent(animation -> animation.createSprites(consumer, direction, frame));
		workingVisualisations.ifPresent(list -> {
			for (FPWorkingVisualisation workingVisualisation : list) {
				if (workingVisualisation.alwaysDraw) {
					workingVisualisation.createSprites(consumer, direction, frame);
				}
			}
		});
	}

	public List<Sprite> createSprites(Direction direction, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, direction, frame);
		return ret;
	}
}
