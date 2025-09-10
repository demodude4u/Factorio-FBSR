package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPBeltReaderLayer;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.map.MapPosition;

public abstract class TransportBeltConnectableRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	// absolute frames
	public static final int BELT_READER_RAIL_N = 0;
	public static final int BELT_READER_RAIL_E = 1;
	public static final int BELT_READER_RAIL_S = 2;
	public static final int BELT_READER_RAIL_W = 3;
	public static final int BELT_READER_BAR_N = 4;
	public static final int BELT_READER_BAR_E = 5;
	public static final int BELT_READER_BAR_S = 6;
	public static final int BELT_READER_BAR_W = 7;
	public static final int BELT_READER_CURVE_NE = 8;
	public static final int BELT_READER_CURVE_SE = 9;
	public static final int BELT_READER_CURVE_SW = 10;
	public static final int BELT_READER_CURVE_NW = 11;
	public static final int BELT_READER_END_S = 12;
	public static final int BELT_READER_END_W = 13;
	public static final int BELT_READER_END_N = 14;
	public static final int BELT_READER_END_E = 15;

	public static final int CONNECTOR_X = 0;
	public static final int CONNECTOR_H = 1;
	public static final int CONNECTOR_V = 2;
	public static final int CONNECTOR_SE = 3;
	public static final int CONNECTOR_SW = 4;
	public static final int CONNECTOR_NE = 5;
	public static final int CONNECTOR_NW = 6;
	
	public static final int[] beltReaderRailLeft = { // cardinal
		BELT_READER_RAIL_W, BELT_READER_RAIL_N, BELT_READER_RAIL_E, BELT_READER_RAIL_S
	};
	public static final int[] beltReaderRailRight = { // cardinal
		BELT_READER_RAIL_E, BELT_READER_RAIL_S, BELT_READER_RAIL_W, BELT_READER_RAIL_N
	};
	public static final int[] beltReaderBarLeft = { // cardinal
		BELT_READER_BAR_W, BELT_READER_BAR_N, BELT_READER_BAR_E, BELT_READER_BAR_S
	};
	public static final int[] beltReaderBarRight = { // cardinal
		BELT_READER_BAR_E, BELT_READER_BAR_S, BELT_READER_BAR_W, BELT_READER_BAR_N
	};
	public static final int[][] beltReaderCurve = { // cardinal, bend
		{ BELT_READER_CURVE_NW, -1, BELT_READER_CURVE_NE }, // North
		{ BELT_READER_CURVE_NE, -1, BELT_READER_CURVE_SE }, // East
		{ BELT_READER_CURVE_SE, -1, BELT_READER_CURVE_SW }, // South
		{ BELT_READER_CURVE_SW, -1, BELT_READER_CURVE_NW }, // West
	};
	public static final int[] beltReaderEnd = { // cardinal
		BELT_READER_END_N, BELT_READER_END_E, BELT_READER_END_S, BELT_READER_END_W
	};

	public static final int[][] connectorCurve = { // cardinal, bend
		{ CONNECTOR_NW, -1, CONNECTOR_NE }, // North
		{ CONNECTOR_NE, -1, CONNECTOR_SE }, // East
		{ CONNECTOR_SE, -1, CONNECTOR_SW }, // South
		{ CONNECTOR_SW, -1, CONNECTOR_NW }, // West
	};

	public static final String[][] indexNames = //
			new String[/* Cardinal */][/* Bend */] { //
					{ "west_to_north_index", "north_index", "east_to_north_index" }, // North
					{ "north_to_east_index", "east_index", "south_to_east_index" }, // East
					{ "east_to_south_index", "south_index", "west_to_south_index" }, // South
					{ "south_to_west_index", "west_index", "north_to_west_index" }, // West
			};

	public static final String[] startingIndexNames = //
			new String[/* Cardinal */] { //
					"starting_north_index", // North
					"starting_east_index", // East
					"starting_south_index", // South
					"starting_west_index", // West
			};
	public static final String[] endingIndexNames = //
			new String[/* Cardinal */] { //
					"ending_north_index", // North
					"ending_east_index", // East
					"ending_south_index", // South
					"ending_west_index", // West
			};

	public static final int[][] indexDefaults = //
			new int[/* Cardinal */][/* Bend */] { //
					{ 6, 2, 4 }, // North
					{ 5, 0, 8 }, // East
					{ 9, 3, 11 }, // South
					{ 10, 1, 7 }, // West
			};
	public static final int[] startingIndexDefaults = //
			new int[/* Cardinal */] { //
					16, // North
					18, // East
					12, // South
					14, // West
			};
	public static final int[] endingIndexDefaults = //
			new int[/* Cardinal */] { //
					17, // North
					19, // East
					13, // South
					15, // West
			};

	private boolean protoHasBeltAnimationSet;
	private FPRotatedAnimation protoBeltAnimationSet;
	private int[][] protoBeltIndices;
	private int[] protoBeltStartingIndices;
	private int[] protoBeltEndingIndices;
	private boolean protoBeltAlternate;
	private List<FPBeltReaderLayer> protoBeltReader;

	protected void defineBeltEndingSprites(Consumer<SpriteDef> consumer, int cardinal, int frame) {
		if (protoHasBeltAnimationSet) {
			protoBeltAnimationSet.defineSprites(consumer, protoBeltEndingIndices[cardinal], frame);
		}
	}

	protected void defineBeltSprites(Consumer<SpriteDef> consumer, int cardinal, int bend, int frame) {
		if (protoHasBeltAnimationSet) {
			protoBeltAnimationSet.defineSprites(consumer, protoBeltIndices[cardinal][bend], frame);
		}
	}

	protected void defineBeltStartingSprites(Consumer<SpriteDef> consumer, int cardinal, int frame) {
		if (protoHasBeltAnimationSet) {
			protoBeltAnimationSet.defineSprites(consumer, protoBeltStartingIndices[cardinal], frame);
		}
	}

	protected void defineBeltReaderSprites(Consumer<LayeredSpriteDef> consumer, int absoluteFrame) {
		if (protoHasBeltAnimationSet) {
			int index = absoluteFrame / 4;
			int frame = absoluteFrame % 4;
			for (FPBeltReaderLayer br : protoBeltReader) {
				br.sprites.defineSprites(s -> consumer.accept(new LayeredSpriteDef(s, br.renderLayer)),
						index, frame);
			}
		}
	}

	protected int getAlternatingFrame(MapPosition pos) {
		if (protoBeltAlternate && (((pos.getXCell() + pos.getYCell()) % 2) == 0)) {
			int frameCount = protoBeltAnimationSet.frameCount;
			return (FRAME + frameCount / 2) % frameCount;
		} else {
			return FRAME;
		}
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		if (protoHasBeltAnimationSet) {
			protoBeltAnimationSet.getDefs(register, FRAME);
			if (protoBeltAlternate) {
				int frameCount = protoBeltAnimationSet.frameCount;
				int altFrame = (FRAME + frameCount / 2) % frameCount;
				protoBeltAnimationSet.getDefs(register, altFrame);
			}

			protoBeltReader.forEach(br -> 
					IntStream.range(0, br.sprites.frameCount)
							.forEach(i -> br.sprites.getDefs(register, i)));
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaBeltAnimationSet = prototype.lua().get("belt_animation_set");

		if (luaBeltAnimationSet.isnil()) {
			protoHasBeltAnimationSet = false;

		} else {
			protoHasBeltAnimationSet = true;
			protoBeltAnimationSet = new FPRotatedAnimation(profile, luaBeltAnimationSet.get("animation_set"));
			protoBeltIndices = new int[4][BeltBend.values().length];
			protoBeltStartingIndices = new int[4];
			protoBeltEndingIndices = new int[4];

			for (int cardinal = 0; cardinal < 4; cardinal++) {
				for (int bend = 0; bend < BeltBend.values().length; bend++) {
					String indexName = indexNames[cardinal][bend];
					int defaultIndex = indexDefaults[cardinal][bend];
					protoBeltIndices[cardinal][bend] = luaBeltAnimationSet.get(indexName).optint(defaultIndex + 1) - 1;
				}
				{
					String indexName = startingIndexNames[cardinal];
					int defaultIndex = startingIndexDefaults[cardinal];
					protoBeltStartingIndices[cardinal] = luaBeltAnimationSet.get(indexName).optint(defaultIndex + 1)
							- 1;
				}
				{
					String indexName = endingIndexNames[cardinal];
					int defaultIndex = endingIndexDefaults[cardinal];
					protoBeltEndingIndices[cardinal] = luaBeltAnimationSet.get(indexName).optint(defaultIndex + 1) - 1;
				}
			}

			protoBeltAlternate = luaBeltAnimationSet.get("alternate").optboolean(false);

			protoBeltReader = FPUtils.list(profile, luaBeltAnimationSet.get("belt_reader"), FPBeltReaderLayer::new);
		}
	}

}
