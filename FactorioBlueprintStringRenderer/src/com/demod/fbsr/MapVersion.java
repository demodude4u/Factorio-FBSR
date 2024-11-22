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
	private final int main;
	private final int major;
	private final int minor;
	private final int dev;

	public MapVersion() {
		this(0);
	}

	public MapVersion(int main, int major, int minor, int dev) {
		this.main = main;
		this.major = major;
		this.minor = minor;
		this.dev = dev;
		this.version = (dev) | ((long) (minor) << 16) | ((long) (major) << 32) | ((long) (main) << 48);
	}

	public MapVersion(long serialized) {
		this.version = serialized;
		main = (int) ((version >> 48) & UINT16_MASK);
		major = (int) ((version >> 32) & UINT16_MASK);
		minor = (int) ((version >> 16) & UINT16_MASK);
		dev = (int) (version & UINT16_MASK);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MapVersion) {
			MapVersion otherVersion = (MapVersion) other;
			return this.version == otherVersion.version;
		}
		return false;
	}

	public int getDev() {
		return dev;
	}

	public int getMain() {
		return main;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public long getSerialized() {
		return version;
	}

	public boolean greaterOrEquals(MapVersion other) {
		return version >= other.version;
	}

	public boolean isInvalid() {
		return version == 0;
	}

	@Override
	public String toString() {
		return "MapVersion: main: " + main + " major: " + major + " minor: " + minor + " dev: " + dev;
	}
}
