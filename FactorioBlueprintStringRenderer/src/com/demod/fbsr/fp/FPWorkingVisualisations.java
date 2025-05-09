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
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPWorkingVisualisations {
	public final Optional<FPAnimation4Way> animation;
	public final Optional<FPAnimation4Way> idleAnimation;
	public final Optional<List<FPWorkingVisualisation>> workingVisualisations;

	public final Optional<FPSprite4Way> integrationPatch;
	public final Layer integrationPatchRenderLayer;

	public FPWorkingVisualisations(LuaValue lua) {
		idleAnimation = FPUtils.opt(lua.get("idle_animation"), FPAnimation4Way::new);
		animation = FPUtils.opt(lua.get("animation"), FPAnimation4Way::new).or(() -> idleAnimation);
		workingVisualisations = FPUtils.optList(lua.get("working_visualisations"), FPWorkingVisualisation::new);

		// Not mentioned in docs, found in py
		integrationPatch = FPUtils.opt(lua.get("integration_patch"), FPSprite4Way::new);
		integrationPatchRenderLayer = FPUtils.optLayer(lua.get("integration_patch_render_layer")).orElse(Layer.LOWER_OBJECT);
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
		integrationPatch.ifPresent(sprite -> sprite.defineSprites(consumer, direction));
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
