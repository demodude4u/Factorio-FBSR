package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;

public class FPWorkingVisualisations {
	public final Optional<FPAnimation4Way> animation;
	public final Optional<FPAnimation4Way> idleAnimation;
	public final Optional<List<FPWorkingVisualisation>> workingVisualisations;

	public final Optional<FPSprite4Way> integrationPatch;

	public FPWorkingVisualisations(LuaValue lua) {
		idleAnimation = FPUtils.opt(lua.get("idle_animation"), FPAnimation4Way::new);
		animation = FPUtils.opt(lua.get("animation"), FPAnimation4Way::new).or(() -> idleAnimation);
		workingVisualisations = FPUtils.optList(lua.get("working_visualisations"), FPWorkingVisualisation::new);

		// Not mentioned in docs, found in py
		integrationPatch = FPUtils.opt(lua.get("integration_patch"), FPSprite4Way::new);
	}

	public void createSprites(Consumer<Sprite> consumer, FactorioData data, Direction direction, int frame) {
		animation.ifPresent(animation -> animation.createSprites(consumer, data, direction, frame));
		workingVisualisations.ifPresent(list -> {
			for (FPWorkingVisualisation workingVisualisation : list) {
				if (workingVisualisation.alwaysDraw) {
					workingVisualisation.createSprites(consumer, data, direction, frame);
				}
			}
		});
		integrationPatch.ifPresent(sprite -> sprite.createSprites(consumer, data, direction));
	}

	public List<Sprite> createSprites(FactorioData data, Direction direction, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, data, direction, frame);
		return ret;
	}
}
