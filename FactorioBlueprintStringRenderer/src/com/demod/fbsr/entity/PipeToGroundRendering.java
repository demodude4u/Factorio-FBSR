package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class PipeToGroundRendering extends EntityRendererFactory {

	public static String[] pipeToGroundCardinalNaming = { //
			"up", "right", "down", "left"//
	};
	private List<SpriteDef> protoSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDefRenderer(protoSprites.get(entity.getDirection().cardinal()), entity,
				protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoSprites = Arrays.stream(pipeToGroundCardinalNaming)
				.map(s -> RenderUtils.getSpriteFromAnimation(prototype.lua().get("pictures").get(s)).get())
				.collect(Collectors.toList());
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setPipe(entity.getPosition(), entity.getDirection());
	}

}
