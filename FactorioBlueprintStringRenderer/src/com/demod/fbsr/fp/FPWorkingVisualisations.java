package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPWorkingVisualisations {
	public final Optional<FPAnimation4Way> animation;
	public final Optional<FPAnimation4Way> idleAnimation;
	public final Optional<List<FPWorkingVisualisation>> workingVisualisations;

	public FPWorkingVisualisations(ModsProfile profile, LuaValue lua) {
		idleAnimation = FPUtils.opt(profile, lua.get("idle_animation"), FPAnimation4Way::new);
		animation = FPUtils.opt(profile, lua.get("animation"), FPAnimation4Way::new).or(() -> idleAnimation);
		workingVisualisations = FPUtils.optList(profile, lua.get("working_visualisations"), FPWorkingVisualisation::new);
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction, int frame) {
		animation.ifPresent(animation -> animation.defineSprites(consumer, direction, frame));
		workingVisualisations.ifPresent(list -> {
			for (FPWorkingVisualisation workingVisualisation : list) {
				if (workingVisualisation.alwaysDraw) {
					workingVisualisation.defineSprites(consumer, direction, frame);
				}
			}
		});
	}

	public List<SpriteDef> defineSprites(Direction direction, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, direction, frame);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register, int frame) {
		for (Direction direction : Direction.cardinals()) {
			defineSprites(register, direction, frame);
		}
	}
}
