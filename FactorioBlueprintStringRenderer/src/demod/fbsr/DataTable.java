package demod.fbsr;

import java.util.HashMap;
import java.util.Map;

import org.luaj.vm2.LuaTable;

public class DataTable {
	private final Map<String, DataPrototype> entities = new HashMap<>();
	private final Map<String, DataPrototype> items = new HashMap<>();
	private final Map<String, DataPrototype> recipes = new HashMap<>();
	private final Map<String, DataPrototype> fluids = new HashMap<>();

	public DataTable(TypeHiearchy typeHiearchy, LuaTable dataLua) {
		LuaTable rawLua = dataLua.get("raw").checktable();
		Utils.forEach(rawLua, v -> {
			Utils.forEach(v.checktable(), protoLua -> {
				// LuaUtils.debugPrintTableStructure("", protoLua);
				// System.exit(1);
				String type = protoLua.get("type").toString();
				String name = protoLua.get("name").toString();
				DataPrototype prototype = new DataPrototype(protoLua.checktable(), name, type);
				if (typeHiearchy.isAssignable("item", type)) {
					items.put(name, prototype);
				} else if (typeHiearchy.isAssignable("recipe", type)) {
					recipes.put(name, prototype);
				} else if (typeHiearchy.isAssignable("entity", type)) {
					entities.put(name, prototype);
				} else if (typeHiearchy.isAssignable("fluid", type)) {
					fluids.put(name, prototype);
				}
			});
		});
	}

	public Map<String, DataPrototype> getEntities() {
		return entities;
	}

	public Map<String, DataPrototype> getFluids() {
		return fluids;
	}

	public Map<String, DataPrototype> getItems() {
		return items;
	}

	public Map<String, DataPrototype> getRecipes() {
		return recipes;
	}
}
