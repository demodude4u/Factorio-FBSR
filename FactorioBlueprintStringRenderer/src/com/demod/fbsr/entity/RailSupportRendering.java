package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.entity.RailRendering.RailDef;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRail;
import com.demod.fbsr.map.MapRenderable;

public class RailSupportRendering extends EntityRendererFactory {
	
	private FPRotatedSprite protoStructure;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {

		MapPosition pos = entity.getPosition();

		Optional<Dir16> railDir = Optional.empty();
		for (MapRail rail : map.getRails()) {
			RailDef def = rail.getDef();

			if (!def.A.elevated || !def.B.elevated) {
				continue;
			}

			if (def.A.pos.add(rail.getPos()).distance(pos) < 0.1) {
				railDir = Optional.of(def.A.dir);
				break;

			} else if (def.B.pos.add(rail.getPos()).distance(pos) < 0.1) {
				railDir = Optional.of(def.B.dir);
				break;
			}
		}

		double orientation = railDir.map(d->d.getOrientation()).orElse(entity.getDirection().getOrientation());

		protoStructure.defineSprites(entity.spriteRegister(register, Layer.OBJECT), orientation);
	}

	@Override
	public void initFromPrototype() {
		protoStructure = new FPRotatedSprite(prototype.lua().get("graphics_set").get("structure"), 16);
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		protoStructure.getDefs(register);
	}
}
