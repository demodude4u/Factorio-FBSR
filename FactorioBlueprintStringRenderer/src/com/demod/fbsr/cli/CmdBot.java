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
        @Option(names = "-ignore-not-ready", description = "Ignore profiles that are not ready", defaultValue = "false") boolean ignoreNotReady
    ) {
        if (!ignoreNotReady && !checkProfilesReady()) {
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

    @Command(name = "start", description = "Start Blueprint Bot as a background service")
    public void startBot(
        @Option(names = "-ignore-not-ready", description = "Ignore profiles that are not ready", defaultValue = "false") boolean ignoreNotReady
    ) {
        if (!ignoreNotReady && !checkProfilesReady()) {
            return;
        }

        LOGGER.info("Starting Blueprint Bot service...");
        try {
            launchDetachedJavaProcess(CmdBot.class, "bot", "run");
            
            while (!RPCService.sendCommand("ping").isPresent()) {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
            
            LOGGER.info("Awaiting for Blueprint Bot service to become healthy...");
            
            List<String> waitStatuses = List.of("idle", "starting");
            String status;
            while (waitStatuses.contains(status = RPCService.<String>sendCommand("status").get())) {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }

            if (status.equals("healthy")) {
                LOGGER.info("Blueprint Bot service started successfully and is healthy.");
            } else {
                LOGGER.error("Blueprint Bot service is {}", status);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Command(name = "stop", description = "Stop Blueprint Bot service")
    public void stopBot() {
        if (RPCService.sendCommand("kill").isPresent()) {
            LOGGER.info("Blueprint Bot service is stopping...");
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
            if (RPCService.sendCommand("ping").isPresent()) {
                LOGGER.warn("Failed to stop Blueprint Bot service. It may still be running.");
            } else {
                LOGGER.info("Blueprint Bot service stopped successfully.");
            }
        } else {
            LOGGER.warn("Failed to stop Blueprint Bot service. It may not be running.");
        }
    }

    @Command(name = "restart", description = "Restart Blueprint Bot service")
    public void restartBot(
        @Option(names = "-ignore-not-ready", description = "Ignore profiles that are not ready", defaultValue = "false") boolean ignoreNotReady
    ) {
        if (!ignoreNotReady && !checkProfilesReady()) {
            return;
        }

        stopBot();
        startBot(ignoreNotReady);
    }

    @Command(name = "status", description = "Get the status of Blueprint Bot service")
    public void status() {
        String status = RPCService.<String>sendCommand("status").orElse("not running");
        LOGGER.info("Blueprint Bot service status: {}", status);
    }

    private static void launchDetachedJavaProcess(Class<?> mainClass, String... args) throws IOException {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String mainClassName = mainClass.getName();

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(mainClassName);
        command.addAll(List.of(args));

        ProcessBuilder builder = new ProcessBuilder(command);

        // Detach process:
        builder.redirectInput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);

        if (isWindows()) {
            // On Windows, use 'cmd /c start' to spawn a detached process
            List<String> winCommand = new ArrayList<>();
            winCommand.add("cmd");
            winCommand.add("/c");
            winCommand.add("start");
            winCommand.add("\"\""); // window title
            winCommand.addAll(command);
            new ProcessBuilder(winCommand).start();
        } else {
            // On Unix, run in background using 'nohup'
            List<String> unixCommand = new ArrayList<>();
            unixCommand.add("nohup");
            unixCommand.addAll(command);
            new ProcessBuilder(unixCommand).start();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private boolean checkProfilesReady() {
        if (Profile.listProfiles().stream().allMatch(p->p.getStatus() == ProfileStatus.READY)) {
            return true;
        }

        System.out.println("Some profiles are not ready. Please ensure all profiles are valid and have been built.");
        for (Profile profile : Profile.listProfiles()) {
            System.out.println("Profile: " + profile.getName() + " (" + profile.getStatus() + ")");
        }
        return false;
    }
}
