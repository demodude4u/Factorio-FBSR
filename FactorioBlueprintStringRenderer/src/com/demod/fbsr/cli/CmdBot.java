package com.demod.fbsr.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.fbsr.Profile;
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.app.RPCService;
import com.demod.fbsr.app.FBSRApps;
import com.google.common.util.concurrent.Uninterruptibles;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "bot", description = "Blueprint Bot commands")
public class CmdBot {
	private static final Logger LOGGER = LoggerFactory.getLogger(CmdBot.class);

    @Command(name = "run", description = "Run Blueprint Bot")
    public void runBot(
        @Option(names = "-ignore-not-ready", description = "Ignore profiles that are not ready", defaultValue = "false") boolean ignoreNotReady,
        @Option(names = "-require-all-enabled", description = "Require all profiles to be enabled and ready before starting the bot", defaultValue = "false") boolean requireAllEnabled
    ) {
        if (!ignoreNotReady && !checkProfilesReady(requireAllEnabled)) {
            return;
        }

        if (!FBSRApps.start()) {
            LOGGER.error("Failed to start Blueprint Bot service. Please check the configuration and try again.");
            return;
        }

        if (!FBSRApps.waitForStopped(true)) {
            LOGGER.error("Unexpected error occurred while waiting for Blueprint Bot service to stop.");
        }
    }

    @Command(name = "kill", description = "Stop Blueprint Bot service")
    public void killBot() {
        if (RPCService.sendCommand("kill").isPresent()) {
            LOGGER.info("Blueprint Bot service is stopping...");
            boolean killed = false;
            for (int i = 0; i < 10; i++) {
                if (!RPCService.sendCommand("ping").isPresent()) {
                    killed = true;
                    break;
                }
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
            if (!killed) {
                LOGGER.warn("Failed to stop Blueprint Bot service. It may still be running.");
            } else {
                LOGGER.info("Blueprint Bot service stopped successfully.");
            }
        } else {
            LOGGER.warn("Failed to stop Blueprint Bot service. It may not be running.");
        }
    }

    @Command(name = "status", description = "Get the status of Blueprint Bot service (idle, starting, healthy, failed, stopped, dead)")
    public void status() {
        String status = RPCService.<String>sendCommand("status").orElse("dead");
        LOGGER.info("Blueprint Bot service status: {}", status);
    }

    private boolean checkProfilesReady(boolean requireAllEnabled) {
        if (requireAllEnabled && !Profile.listProfiles().stream().allMatch(p -> p.isEnabled())) {
            System.out.println("Some profiles are not enabled. Please ensure all profiles are valid and have been built.");
            for (Profile profile : Profile.listProfiles()) {
                System.out.println("Profile: " + profile.getName() + " (" + profile.getStatus() + ")");
            }
            return false;
        }

        if (!Profile.listProfiles().stream().allMatch(p->p.getStatus() == ProfileStatus.READY)) {
            System.out.println("Some profiles are not ready. Please ensure all profiles are valid and have been built.");
            for (Profile profile : Profile.listProfiles()) {
                System.out.println("Profile: " + profile.getName() + " (" + profile.getStatus() + ")");
            }
            return false;
        }
        
        return true;
    }
}
