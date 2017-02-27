package demod.fbsr;

import org.luaj.vm2.LuaTable;

public class DataPrototype {
	private final LuaTable lua;
	private final String name;
	private final String type;

	public DataPrototype(LuaTable lua, String name, String type) {
		this.lua = lua;
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public LuaTable lua() {
		return lua;
	}
}