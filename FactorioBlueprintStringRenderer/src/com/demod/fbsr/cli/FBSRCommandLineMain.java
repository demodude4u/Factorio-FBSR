package com.demod.fbsr.cli;

import com.demod.factorio.Config;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "FBSR", mixinStandardHelpOptions = true, subcommands = {
    CmdProfile.class, 
    CmdBot.class, 
    CmdRender.class
})
public class FBSRCommandLineMain {

    @Option(names = "-config", description = "Path to the configuration file", defaultValue = "config.json", scope = ScopeType.INHERIT)
    public void setConfigPath(String configPath) {
        Config.setPath(configPath);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new FBSRCommandLineMain()).execute(args));
    }
}
