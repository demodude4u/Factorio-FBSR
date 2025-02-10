package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.google.common.collect.ImmutableList;

public abstract class TransportBeltConnectableRendering<E extends BSEntity> extends EntityRendererFactory<E> {

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

	// XXX I'm not using horizontal or vertical frames
	public static final int[][] transportBeltConnectorFrameMappingIndex = //
			new int[/* Cardinal */][/* Bend */] { //
					{ 6, 0, 5 }, // North
					{ 5, 0, 3 }, // East
					{ 3, 0, 4 }, // South
					{ 4, 0, 6 }, // West
			};

	private boolean protoHasBeltAnimationSet;
	private FPRotatedAnimation protoBeltAnimationSet;
	private int[][] protoBeltIndices;
	private int[] protoBeltStartingIndices;
	private int[] protoBeltEndingIndices;
	private boolean protoBeltAlternate;

	protected List<Sprite> createBeltEndingSprites(int cardinal, int frame) {
		if (protoHasBeltAnimationSet) {
			return protoBeltAnimationSet.createSprites(data, protoBeltEndingIndices[cardinal], frame);
		} else {
			return ImmutableList.of();
		}
	}

	protected List<Sprite> createBeltSprites(int cardinal, int bend, int frame) {
		if (protoHasBeltAnimationSet) {
			return protoBeltAnimationSet.createSprites(data, protoBeltIndices[cardinal][bend], frame);
		} else {
			return ImmutableList.of();
		}
	}

	protected List<Sprite> createBeltStartingSprites(int cardinal, int frame) {
		if (protoHasBeltAnimationSet) {
			return protoBeltAnimationSet.createSprites(data, protoBeltStartingIndices[cardinal], frame);
		} else {
			return ImmutableList.of();
		}
	}

	protected int getAlternatingFrame(Point2D.Double pos, int frame) {
		if (protoBeltAlternate && ((((int) pos.x + (int) pos.y) % 2) == 0)) {
			int frameCount = protoBeltAnimationSet.getFrameCount();
			return (frame + frameCount / 2) % frameCount;
		} else {
			return frame;
		}
	}

	@Override
	public void initFromPrototype() {

		LuaValue luaBeltAnimationSet = prototype.lua().get("belt_animation_set");

		if (luaBeltAnimationSet.isnil()) {
			protoHasBeltAnimationSet = false;

		} else {
			protoHasBeltAnimationSet = true;
			protoBeltAnimationSet = new FPRotatedAnimation(luaBeltAnimationSet.get("animation_set"));
			protoBeltIndices = new int[4][BeltBend.values().length];
			protoBeltStartingIndices = new int[4];
			protoBeltEndingIndices = new int[4];

			for (int cardinal = 0; cardinal < 4; cardinal++) {
				for (int bend = 0; bend < BeltBend.values().length; bend++) {
					String indexName = indexNames[cardinal][bend];
					int defaultIndex = indexDefaults[cardinal][bend];
					protoBeltIndices[cardinal][bend] = luaBeltAnimationSet.get(indexName).optint(defaultIndex);
				}
				{
					String indexName = startingIndexNames[cardinal];
					int defaultIndex = startingIndexDefaults[cardinal];
					protoBeltStartingIndices[cardinal] = luaBeltAnimationSet.get(indexName).optint(defaultIndex);
				}
				{
					String indexName = endingIndexNames[cardinal];
					int defaultIndex = endingIndexDefaults[cardinal];
					protoBeltEndingIndices[cardinal] = luaBeltAnimationSet.get(indexName).optint(defaultIndex);
				}
			}

			protoBeltAlternate = luaBeltAnimationSet.get("alternate").optboolean(false);
		}
	}

}
