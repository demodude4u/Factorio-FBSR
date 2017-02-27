package demod.fbsr;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class TypeHiearchy {
	private final Map<String, String> parents = new HashMap<>();

	public TypeHiearchy(JSONObject json) {
		for (String key : json.keySet()) {
			JSONObject typeJson = json.getJSONObject(key);
			parents.put(key, typeJson.optString("parent"));
		}
	}

	public boolean isAssignable(String type, String subType) {
		String checkType = subType;
		while (checkType != null) {
			if (type.equals(checkType)) {
				return true;
			}
			checkType = parents.get(checkType);
		}
		return false;
	}
}
