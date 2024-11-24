package com.demod.fbsr.bs;

import org.json.JSONArray;

public class BSWire {
	public final int firstEntityNumber;
	public final int firstWireConnectorId;
	public final int secondEntityNumber;
	public final int secondWireConnectorId;

	public BSWire(JSONArray json) {
		firstEntityNumber = json.getInt(0);
		firstWireConnectorId = json.getInt(1);
		secondEntityNumber = json.getInt(2);
		secondWireConnectorId = json.getInt(3);
	}
}
