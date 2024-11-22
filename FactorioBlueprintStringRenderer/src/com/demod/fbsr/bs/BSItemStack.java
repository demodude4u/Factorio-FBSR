package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSItemStack {
	public final BSSignalID id;
	public final List<BSItemStackItem> itemsInInventory;

	public BSItemStack(JSONObject json) {
		id = new BSSignalID(json.getJSONObject("id"));
		itemsInInventory = BSUtils.list(json.getJSONObject("items"), "in_inventory", BSItemStackItem::new);
	}
}
