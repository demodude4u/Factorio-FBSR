package com.demod.fbsr;

public class MapVersion {
	private static final int UINT16_MASK = 0xFFFF;

	// returns the greater of the two map versions
	public static MapVersion max(MapVersion a, MapVersion b) {
		if (a.greaterOrEquals(b))
			return a;
		return b;
	}

	private final long version;

	public MapVersion() {
		this.version = 0;
	}

	public MapVersion(int main, int major, int minor, int dev) {
		this.version = (dev) | ((long) (minor) << 16) | ((long) (major) << 32) | ((long) (main) << 48);
	}

	public MapVersion(long serialized) {
		this.version = serialized;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MapVersion) {
			MapVersion otherVersion = (MapVersion) other;
			return this.version == otherVersion.version;
		}
		return false;
	}

	public long getSerialized() {
		return this.version;
	}

	public boolean greaterOrEquals(MapVersion other) {
		return this.version >= other.version;
	}

	public boolean isEmpty() {
		return this.version == 0;
	}

	@Override
	public String toString() {
		return "MapVersion: main: " + ((version >> 48) & UINT16_MASK) + " major: " + ((version >> 32) & UINT16_MASK)
				+ " minor: " + ((version >> 16) & UINT16_MASK) + " dev: " + (version & UINT16_MASK);
	}
}
