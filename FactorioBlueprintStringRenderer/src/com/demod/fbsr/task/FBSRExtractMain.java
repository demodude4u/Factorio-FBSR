package com.demod.fbsr.task;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;

public class FBSRExtractMain {

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

//		try (PrintStream ps = new PrintStream("proto.txt")) {
//			Utils.debugPrintLua(table.getTile("refined-hazard-concrete-left").get().lua(), ps);
//		}
//		Desktop.getDesktop().edit(new File("proto.txt"));

		table.getTiles().entrySet().stream()
				.collect(Collectors.groupingBy(e -> e.getValue().lua().get("layer").checkint())).entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getKey())).forEach(e -> System.out.println(e.getKey() + ": "
						+ e.getValue().stream().map(e2 -> e2.getKey()).collect(Collectors.joining(", ", "[", "]"))));
	}

}
