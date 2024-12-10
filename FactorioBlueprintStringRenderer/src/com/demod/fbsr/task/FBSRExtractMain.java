package com.demod.fbsr.task;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;

public class FBSRExtractMain {

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

		LuaValue luaVirtualSignal = table.getRaw("virtual-signal").get();
		Utils.forEach(luaVirtualSignal, (k, v) -> {
			System.out.println(k.tojstring() + " -- " + v.get("icon").tojstring());
		});

//		try (PrintStream ps = new PrintStream("proto.txt")) {
//			Utils.debugPrintLua(table.getTile("stone-path").get().lua(), ps);
//		}
//		Desktop.getDesktop().edit(new File("proto.txt"));

//		table.getTiles().entrySet().stream()
//				.collect(Collectors.groupingBy(e -> e.getValue().lua().get("layer").checkint())).entrySet().stream()
//				.sorted(Comparator.comparing(e -> e.getKey())).forEach(e -> System.out.println(e.getKey() + ": "
//						+ e.getValue().stream().map(e2 -> e2.getKey()).collect(Collectors.joining(", ", "[", "]"))));
	}

}
