package com.demod.fbsr.entity;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSprite;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("pipe")
public class PipeRendering extends EntityWithOwnerRendering {

	public static final int ADJCODE_STRAIGHT_HORIZONTAL = 0b1010;
	public static final int ADJCODE_STRAIGHT_VERTICAL = 0b0101;
	public static final int ADJCODE_STRAIGHT_HORIZONTAL_WINDOW = 16;
	public static final int ADJCODE_STRAIGHT_VERTICAL_WINDOW = 17;

	public static final String[] pipeSpriteNameMapping = //
			new String[/* bits WSEN */] { //
					"straight_horizontal", // ....
					"ending_up", // ...N
					"ending_right", // ..E.
					"corner_up_right", // ..EN
					"ending_down", // .S..
					"straight_vertical", // .S.N
					"corner_down_right", // .SE.
					"t_right", // .SEN
					"ending_left", // W...
					"corner_up_left", // W..N
					"straight_horizontal", // W.E.
					"t_up", // W.EN
					"corner_down_left", // WS..
					"t_left", // WS.N
					"t_down", // WSE.
					"cross",// WSEN
					"straight_horizontal_window", // Special 16
					"straight_vertical_window", // Special 17
			};

	private List<FPSprite> protoPipeSprites;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		MapPosition pos = entity.getPosition();

		int adjCode = map.getPipePieceAdjCode(pos).getAsInt();

		boolean sh = adjCode == ADJCODE_STRAIGHT_HORIZONTAL;
		boolean sv = adjCode == ADJCODE_STRAIGHT_VERTICAL;
		if (sh || sv) {
			boolean window = true;
			Direction checkDir = sh ? Direction.EAST : Direction.SOUTH;
			int checkStraightAdjCode = sh ? ADJCODE_STRAIGHT_HORIZONTAL : ADJCODE_STRAIGHT_VERTICAL;
			MapPosition fwdPos = checkDir.offset(pos);
			MapPosition revPos = checkDir.back().offset(pos);
			OptionalInt fwdAdjCode = map.getPipePieceAdjCode(fwdPos);
			OptionalInt revAdjCode = map.getPipePieceAdjCode(revPos);
			if ((fwdAdjCode.isPresent() && fwdAdjCode.getAsInt() == checkStraightAdjCode) &&
					(revAdjCode.isPresent() && revAdjCode.getAsInt() == checkStraightAdjCode)) {
				int sum = pos.getXCell() + pos.getYCell();
				window = Math.floorMod(sum, 2) != 0;
			}
			if (window) {
				adjCode = sh ? ADJCODE_STRAIGHT_HORIZONTAL_WINDOW : ADJCODE_STRAIGHT_VERTICAL_WINDOW;
			}
		}

		protoPipeSprites.get(adjCode).defineSprites(entity.spriteRegister(register, Layer.OBJECT));
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.fluidBox(lua.get("fluid_box")).ignorePipeCovers();
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPipeSprites.forEach(fp -> fp.defineSprites(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaPictures = prototype.lua().get("pictures");
		protoPipeSprites = Arrays.stream(pipeSpriteNameMapping).map(s -> new FPSprite(profile, luaPictures.get(s)))
				.collect(Collectors.toList());
	}

	public boolean pipeFacingMeFrom(Direction direction, WorldMap map, MapEntity entity) {
		return map.isPipeConnected(entity.getPosition(), direction);
	}

	@Override
	public void populateLogistics(WorldMap map, MapEntity entity) {
		super.populateLogistics(map, entity);

		MapPosition pos = entity.getPosition();
		int adjCode = 0;
		adjCode |= ((map.isPipeConnected(pos, Direction.NORTH) ? 1 : 0) << 0);
		adjCode |= ((map.isPipeConnected(pos, Direction.EAST) ? 1 : 0) << 1);
		adjCode |= ((map.isPipeConnected(pos, Direction.SOUTH) ? 1 : 0) << 2);
		adjCode |= ((map.isPipeConnected(pos, Direction.WEST) ? 1 : 0) << 3);
		map.setPipePieceAdjCode(entity.getPosition(), adjCode);
	}
}
