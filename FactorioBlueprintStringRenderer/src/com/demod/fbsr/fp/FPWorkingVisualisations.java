package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;
import com.google.common.collect.ImmutableList;

public class FPWorkingVisualisations {
	public final Optional<FPAnimation4Way> animation;
	public final Optional<FPAnimation4Way> idleAnimation;
	public final Optional<List<Optional<FPWorkingVisualisation>>> workingVisualisations;

	public final Optional<FPSprite4Way> integrationPatch;

	public FPWorkingVisualisations(LuaValue lua) {
		idleAnimation = FPUtils.opt(lua.get("idle_animation"), FPAnimation4Way::new);
		animation = FPUtils.opt(lua.get("animation"), FPAnimation4Way::new).or(() -> idleAnimation);

		// TODO there are bugs in factorio about this structure
		LuaValue luaWorkingVisualisations = lua.get("working_visualisations");
		if (checkBugInWorkingVisualisations(luaWorkingVisualisations)) {
			workingVisualisations = Optional.empty();
		} else {
			workingVisualisations = Optional.of(
					ImmutableList.of(Optional.of(new FPWorkingVisualisation(luaWorkingVisualisations.get("layer")))));
		}

		// Not mentioned in docs, found in py
		integrationPatch = FPUtils.opt(lua.get("integration_patch"), FPSprite4Way::new);
	}

	private boolean checkBugInWorkingVisualisations(LuaValue lua) {
		if (lua.isnil()) {
			return false;
		}
		if (lua.isarray()) {
			return false;
		}
		// Detect any keys that are not integers
		return ((JSONObject) lua.getJson()).keySet().stream().anyMatch(s -> {
			try {
				Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return true;
			}
			return false;
		});
	}

	public void createSprites(Consumer<Sprite> consumer, FactorioData data, Direction direction, int frame) {
		animation.ifPresent(animation -> animation.createSprites(consumer, data, direction, frame));
		workingVisualisations.ifPresent(list -> {
			for (Optional<FPWorkingVisualisation> workingVisualisation : list) {
				if (!workingVisualisation.isPresent()) {
					continue;
				}
				if (workingVisualisation.get().alwaysDraw) {
					workingVisualisation.get().createSprites(consumer, data, direction, frame);
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
