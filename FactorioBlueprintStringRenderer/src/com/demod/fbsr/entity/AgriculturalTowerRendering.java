package com.demod.fbsr.entity;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAgriculturalCraneProperties;
import com.demod.fbsr.fp.FPCranePart;
import com.demod.fbsr.fp.FPVector3D;
import com.demod.fbsr.fp.FPWorkingVisualisations;
import com.demod.fbsr.map.MapCranePart;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.google.common.collect.ImmutableMap;

@EntityType("agricultural-tower")
public class AgriculturalTowerRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;
	// private static final float CRANE_ORIENTATION = 0.75f;
	// private static final float CRANE_EXTENT = 0.5f;

	private FPWorkingVisualisations protoGraphicsSet;
	private FPAgriculturalCraneProperties protoCrane;

	
	public static class ShamefulHardcoding {
		public static final Map<String, ShamefulHardcoding> DATA = ImmutableMap.<String, ShamefulHardcoding>builder()
				.put("hub", data(32, -203, 0, 270))
				.put("arm_inner", data(-28, -241, 298, 270))
				.put("arm_inner_joint", data(-137, -280, 275, 270))
				// .put("arm_central", data(0, 0, 0.75))
				.put("arm_central_joint", data(-191, -283, 262, 270))
				.put("arm_outer", data(-9, -308, 82, 270, 1, 0.8))
				.put("grappler-hub", data(-304, -255, 180, 270, 0.75))
				.put("telescope", data(-304, -194, 180, 270, 0.75))
				.put("grappler-claw", data(-304, -102, 180, 270))
				.build();

		public MapPosition pos;
		public double rotationOrientation;
		public double spriteOrientation;
		public double scaleHeight;
		public double cropStart;

		private static ShamefulHardcoding data(int px, int py, double rA, double sA) {
			return data(px, py, rA, sA, 1.0);
		}

		private static ShamefulHardcoding data(int px, int py, double rA, double sA, double scale) {
			return data(px, py, rA, sA, scale, 0.0);
		}

		private static ShamefulHardcoding data(int px, int py, double rA, double sA, double scale, double cropStart) {
			ShamefulHardcoding data = new ShamefulHardcoding();
			data.pos = MapPosition.byPixel(px, py);
			data.rotationOrientation = rA / 360.0;
			data.spriteOrientation = sA / 360.0;
			data.scaleHeight = scale;
			data.cropStart = cropStart;
	
			return data;
		}
	}	

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		protoGraphicsSet.defineSprites(entity.spriteRegister(register, Layer.OBJECT), entity.getDirection(), FRAME);
		
		// FPVector3D cranePoint = protoCrane.origin;
		// FPVector3D craneGrapplerPoint = cranePoint;

		for (FPCranePart part : protoCrane.parts) {
			// FPVector3D rp = part.relativePosition;
			// FPVector3D rpg = part.relativePositionGrappler;

			// FPVector3D sl = part.staticLength;
			// FPVector3D exl = part.extendableLength;
			// FPVector3D extent = new FPVector3D(
			// 		sl.x + exl.x * CRANE_EXTENT, 
			// 		sl.y + exl.y * CRANE_EXTENT,
			// 		sl.z + exl.z * CRANE_EXTENT);

			// FPVector3D slg = part.staticLengthGrappler;
			// FPVector3D exlg = part.extendableLengthGrappler;
			// FPVector3D grapplerExtent = new FPVector3D(
			// 		slg.x + exlg.x * CRANE_EXTENT, 
			// 		slg.y + exlg.y * CRANE_EXTENT,
			// 		slg.z + exlg.z * CRANE_EXTENT);

			// cranePoint = cranePoint.add(rp.x, rp.y, rp.z);
			// craneGrapplerPoint = craneGrapplerPoint.add(rpg.y, rpg.x, rpg.z);

			ShamefulHardcoding shc = ShamefulHardcoding.DATA.get(part.name);
			if (shc == null) {
				// cranePoint = cranePoint.add(extent.x, extent.y, extent.z);
				// craneGrapplerPoint = craneGrapplerPoint.add(grapplerExtent.y, grapplerExtent.x, grapplerExtent.z);
				continue;
			}

			// FPVector3D point = cranePoint;
			// if (part.isGrappler()) {
			// 	point = point.add(craneGrapplerPoint);
			// }

			// MapPosition pos = MapPosition3D.convert(cranePoint).flatten();
			// MapPosition pos = entity.getPosition()
			// 		.add(MapPosition.byUnit(-point.y, (point.x - point.z) * FPUtils.PROJECTION_CONSTANT))
			// 		.add(hardcodedShift);
			MapPosition pos = shc.pos.add(entity.getPosition());
			
			// System.out.println(part.name+": "+pos.toPixels());

			part.sprite.ifPresent(fp -> fp.defineSprites(s -> register.accept(new MapCranePart(s, pos, shc.rotationOrientation, shc.scaleHeight, shc.cropStart, part))));
			// double rotatedOrientation = part.allowSpriteRotation ? CRANE_ORIENTATION : 0;
			part.rotatedSprite.ifPresent(fp -> fp.defineSprites(s -> register.accept(new MapCranePart(s, pos, shc.rotationOrientation, shc.scaleHeight, shc.cropStart, part)), shc.spriteOrientation));

			// cranePoint = cranePoint.add(extent.x, extent.y, extent.z);
			// craneGrapplerPoint = craneGrapplerPoint.add(grapplerExtent.y, grapplerExtent.x, grapplerExtent.z);
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.circuitConnector(lua.get("circuit_connector"));
		bind.energySource(lua.get("energy_source"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoGraphicsSet.getDefs(register, FRAME);

		//Limit frames to the orientations used in hardcoded data
		protoCrane.getDefs(register, ShamefulHardcoding.DATA);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoGraphicsSet = new FPWorkingVisualisations(profile, prototype.lua().get("graphics_set"));
		protoCrane = new FPAgriculturalCraneProperties(profile, prototype.lua().get("crane"));
	}

}
