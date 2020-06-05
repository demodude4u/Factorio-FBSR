package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.google.common.collect.ImmutableList;

public class ElectricEnergyInterfaceRendering extends EntityRendererFactory {
	private static List<String> imageProperties = ImmutableList.of("picture", "pictures", "animation", "animations");

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {

		Optional<LuaValue> findImageLua = imageProperties.stream().map(p -> prototype.lua().get(p))
				.filter(l -> l != LuaValue.NIL).findAny();

		if (findImageLua.isPresent()) {
			List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(findImageLua.get(), entity.getDirection());
			register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
		}

	}
}
