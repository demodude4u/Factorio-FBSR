package com.demod.fbsr.cli;

import com.demod.factorio.Config;
import com.demod.fbsr.app.StartAllServices;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "bot", description = "Blueprint Bot commands")
public class CmdBot {

    @Command(name = "run", description = "Run Blueprint Bot")
    public void runBot() {
        StartAllServices.main(null);
    }

    @Command(name = "start", description = "Start Blueprint Bot as a background service")
    public void startBot() {
        //TODO
    }

    @Command(name = "stop", description = "Stop Blueprint Bot service")
    public void stopBot() {
        //TODO
    }
}
