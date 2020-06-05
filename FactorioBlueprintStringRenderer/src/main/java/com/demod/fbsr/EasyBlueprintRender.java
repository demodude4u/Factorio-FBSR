package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.json.JSONException;

public class EasyBlueprintRender {

	public static byte[] renderBlueprint(final String rawBlueprintString) throws JSONException, IOException {
		final TaskReporting taskReporting = new TaskReporting();
		final Blueprint bp = new Blueprint(BlueprintStringData.decode(rawBlueprintString));
		final BufferedImage img = FBSR.renderBlueprint(bp, taskReporting);

		final byte[] imageBytes;
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(img, "png", baos);
			baos.flush();
			imageBytes = baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

//		try (final OutputStream os = new FileOutputStream(new File("G:\\Blueprint Bot Images\\bp.png"))) {
//			os.write(imageBytes);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		return imageBytes;
	}
}
