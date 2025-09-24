package com.demod.fbsr.entity;

import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

@EntityType("wall")
public class WallRendering extends EntityWithOwnerRendering {

	public static final String[] wallSpriteNameMapping = //
			new String[/* bits WSEN */] { //
					"single", // ....
					"single", // ...N
					"ending_right", // ..E.
					"ending_right", // ..EN
					"straight_vertical", // .S..
					"straight_vertical", // .S.N
					"corner_right_down", // .SE.
					"corner_right_down", // .SEN
					"ending_left", // W...
					"ending_left", // W..N
					"straight_horizontal", // W.E.
					"straight_horizontal", // W.EN
					"corner_left_down", // WS..
					"corner_left_down", // WS.N
					"t_up", // WSE.
					"t_up",// WSEN
			};
	private List<FPSpriteVariations> protoPictures;
	private Optional<FPSpriteVariations> protoFilling;
	private Optional<FPSprite4Way> protoWallDiodeRed;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();

		boolean northGate = map.isVerticalGate(Direction.NORTH.offset(pos));
		boolean eastGate = map.isHorizontalGate(Direction.EAST.offset(pos));
		boolean southGate = map.isVerticalGate(Direction.SOUTH.offset(pos));
		boolean westGate = map.isHorizontalGate(Direction.WEST.offset(pos));

		boolean wallN = map.isWall(Direction.NORTH.offset(pos));
		boolean wallE = map.isWall(Direction.EAST.offset(pos));
		boolean wallS = map.isWall(Direction.SOUTH.offset(pos));
		boolean wallW = map.isWall(Direction.WEST.offset(pos));
		boolean wallNE = map.isWall(Direction.NORTHEAST.offset(pos));
		boolean fill = wallE && wallN && wallNE;

		int adjCode = 0;
		adjCode |= ((wallN || northGate ? 1 : 0) << 0);
		adjCode |= ((wallE || eastGate ? 1 : 0) << 1);
		adjCode |= ((wallS || southGate ? 1 : 0) << 2);
		adjCode |= ((wallW || westGate ? 1 : 0) << 3);

		FPSpriteVariations wallSprites = protoPictures.get(adjCode);

		if (fill && protoFilling.isPresent()) {
			int variation;
			FPSpriteVariations fillingSprites = protoFilling.get();
			if (fillingSprites.getVariationCount() > 1) {
				variation = Math.abs(pos.getXCell() + pos.getYCell()) % (fillingSprites.getVariationCount() / 2);
			} else {
				variation = 0;
			}
			fillingSprites.defineSprites(s -> register.accept(new MapSprite(s, Layer.OBJECT, entity.getPosition().addUnit(0.5, -1))), variation);
		}

		{
			int variation;
			if (wallSprites.getVariationCount() > 1) {
				variation = Math.abs(pos.getXCell() + pos.getYCell()) % (wallSprites.getVariationCount() / 2);
			} else {
				variation = 0;
			}
			wallSprites.defineSprites(entity.spriteRegister(register, Layer.OBJECT), variation);
		}

		if (northGate || eastGate || southGate || westGate) {
			protoWallDiodeRed.ifPresent(fp -> fp.defineSprites(entity.spriteRegister(register, Layer.OBJECT), entity.getDirection()));
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

		protoPictures.forEach(fp -> fp.getDefs(register));
		protoFilling.ifPresent(fp -> fp.getDefs(register));
		protoWallDiodeRed.ifPresent(fp -> fp.getDefs(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaPictures = prototype.lua().get("pictures");
		protoPictures = Arrays.stream(wallSpriteNameMapping).map(s -> new FPSpriteVariations(profile, luaPictures.get(s)))
				.collect(Collectors.toList());
		
		LuaValue luaFilling = luaPictures.get("filling");
		if (!luaFilling.isnil()) {
			protoFilling = Optional.of(new FPSpriteVariations(profile, luaFilling));
		} else {
			protoFilling = Optional.empty();
		}
		
		protoWallDiodeRed = FPUtils.opt(profile, prototype.lua().get("wall_diode_red"), FPSprite4Way::new);
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		map.setWall(entity.getPosition());
	}
}
