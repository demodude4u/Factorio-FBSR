package com.demod.fbsr.task;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;

public class FBSRExtractMain {

	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

		table.getEntities().values().stream().filter(e -> e.getType().equals("ammo-turret")).map(e -> e.getName())
				.forEach(System.out::println);

//		table.getTiles().entrySet().stream()
//				.filter(e -> TileRendererFactory.forType(e.getValue().getType()) != TileRendererFactory.UNKNOWN)
//				.map(e -> e.getValue().getType() + " -> " + e.getKey()).sorted().forEach(System.out::println);
	}

}
