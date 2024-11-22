package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSItemStack {
	public final String idName;
	public final List<BSItemStackItem> itemsInInventory;

	public BSItemStack(JSONObject json) {
		// XXX suspicious json structure
		idName = json.getJSONObject("id").getString("name");
		itemsInInventory = BSUtils.list(json.getJSONObject("items"), "in_inventory", BSItemStackItem::new);
	}
}
