package com.demod.fbsr.task;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintStream;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;

public class FBSRExtractMain {

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

		try (PrintStream ps = new PrintStream("proto.txt")) {
			Utils.debugPrintLua(table.getEntity("decider-combinator").get().lua(), ps);
		}
		Desktop.getDesktop().edit(new File("proto.txt"));
	}

}
