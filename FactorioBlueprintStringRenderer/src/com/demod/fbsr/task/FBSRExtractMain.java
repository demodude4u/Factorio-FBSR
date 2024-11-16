package com.demod.fbsr.task;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintStream;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;

public class FBSRExtractMain {

	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

//		table.getEntities().keySet().stream().sorted().forEach(System.out::println);

		try (PrintStream ps = new PrintStream("proto.txt")) {
			Utils.debugPrintLua(table.getEntity("rail-chain-signal").get().lua(), ps);
		}
		Desktop.getDesktop().edit(new File("proto.txt"));

//		table.getEntities().values().stream().filter(e -> e.getType().equals("ammo-turret")).map(e -> e.getName())
//				.forEach(System.out::println);

//		table.getTiles().entrySet().stream()
//				.filter(e -> TileRendererFactory.forType(e.getValue().getType()) != TileRendererFactory.UNKNOWN)
//				.map(e -> e.getValue().getType() + " -> " + e.getKey()).sorted().forEach(System.out::println);
	}

}
