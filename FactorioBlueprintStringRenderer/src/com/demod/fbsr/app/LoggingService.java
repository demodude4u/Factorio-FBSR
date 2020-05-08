package com.demod.fbsr.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.swing.JOptionPane;

import org.apache.commons.io.output.TeeOutputStream;
import org.json.JSONObject;

import com.demod.factorio.Config;
import com.google.common.util.concurrent.AbstractIdleService;

public class LoggingService extends AbstractIdleService {

	static {
		PrintStream err = System.err;
		try {
			JSONObject configJson = Config.get().getJSONObject("logging");

			File file = new File(configJson.getString("file"));
			FileOutputStream fos = new FileOutputStream(file);
			System.setOut(new PrintStream(new TeeOutputStream(System.out, fos)));
			System.setErr(new PrintStream(new TeeOutputStream(System.err, fos)));

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace(err);
			throw new InternalError(e);
		}
	}

	@Override
	protected void shutDown() {
		ServiceFinder.removeService(this);
	}

	@Override
	protected void startUp() {
		ServiceFinder.addService(this);

	}

}
