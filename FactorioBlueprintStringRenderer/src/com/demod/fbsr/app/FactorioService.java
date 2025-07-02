package com.demod.fbsr.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.FBSR;
import com.google.common.util.concurrent.AbstractIdleService;

public class FactorioService extends AbstractIdleService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FactorioService.class);

    @Override
    protected void startUp() throws Exception {
        if (!FBSR.initialize()) {
			throw new RuntimeException("Failed to initialize FBSR.");
		}
    }

    @Override
    protected void shutDown() throws Exception {
        if (!FBSR.unload()) {
			throw new RuntimeException("Failed to shutdown FBSR.");
		}
    }
}
