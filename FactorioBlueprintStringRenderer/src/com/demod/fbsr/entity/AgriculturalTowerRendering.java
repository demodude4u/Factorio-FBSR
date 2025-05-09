package com.demod.fbsr.entity;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAgriculturalCraneProperties;
import com.demod.fbsr.fp.FPCranePart;
import com.demod.fbsr.fp.FPVector3D;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public class AgriculturalTowerRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;
	private static final float CRANE_ORIENTATION = 0.75f;

	private FPWorkingVisualisations protoGraphicsSet;
	private FPAgriculturalCraneProperties protoCrane;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		protoGraphicsSet.defineSprites(entity.spriteRegister(register, Layer.OBJECT), entity.getDirection(), FRAME);

		BiFunction<MapPosition3D, Layer, Consumer<SpriteDef>> craneRegister = (p, l) -> (s -> register.accept(new MapSprite(s, l, p.flatten())));
		FPVector3D cranePoint = protoCrane.origin;
		FPVector3D craneGrapplerPoint = protoCrane.origin;

		for (FPCranePart part : protoCrane.parts) {
			FPVector3D rp = part.relativePosition;
			FPVector3D rpg = part.relativePositionGrappler;
			FPVector3D sl = part.staticLength;
			FPVector3D slg = part.staticLengthGrappler;

			cranePoint = cranePoint.add(rp.y, rp.x, rp.z);
			craneGrapplerPoint = craneGrapplerPoint.add(rpg.y, rpg.x, rpg.z);

			final MapPosition3D p = MapPosition3D.convert(cranePoint);
			part.sprite.ifPresent(fp -> fp.defineSprites(craneRegister.apply(p, part.layer)));
			part.rotatedSprite.ifPresent(fp -> fp.defineSprites(craneRegister.apply(p, part.layer), CRANE_ORIENTATION));

			cranePoint = cranePoint.add(sl.y, sl.x, sl.z);
			craneGrapplerPoint = craneGrapplerPoint.add(slg.y, slg.x, slg.z);
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.circuitConnector(lua.get("circuit_connector"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoGraphicsSet.getDefs(register, FRAME);
		protoCrane.getDefs(register, CRANE_ORIENTATION);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(prototype.lua().get("graphics_set"));
		protoCrane = new FPAgriculturalCraneProperties(prototype.lua().get("crane"));
	}

}
