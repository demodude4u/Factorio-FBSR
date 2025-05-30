package com.demod.fbsr.cli;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.rapidoid.annotation.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.app.RPCService;
import com.demod.fbsr.app.StartAllServices;
import com.google.common.util.concurrent.Uninterruptibles;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rpc", description = "Internal commands for RPC (Remote Procedure Call) operations")
public class CmdRPC {
	private static final Logger LOGGER = LoggerFactory.getLogger(CmdRPC.class);

    public static final long START_STAMP = System.currentTimeMillis();

    private volatile boolean killRequested = false;

    @Command(name = "ping")
    public long ping() {
        return System.currentTimeMillis() - START_STAMP;
    }

    @Command(name = "status")
    public String status() {
        return StartAllServices.status;
    }

    @Command(name = "kill")
    public boolean kill() {
        if (killRequested) {
            return false;
        }
        killRequested = true;
        LOGGER.info("Kill command received, shutting down...");
        new Thread(() -> {
            Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Goodbye!");
            System.exit(0);
        }).start();
        return true;
    }

    @Command(name = "render")
    public static class SubCmdRender {

        @Parameters
        public String blueprintString;
        
        @Parameters(arity = "0..1")
        public Optional<String> options;

        @Command(name = "preview")
        public JSONObject preview() {

        }

        @Command(name = "full")
        public JSONObject full() {

        }

    }

}
