package com.demod.fbsr.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "FBSR", mixinStandardHelpOptions = true, subcommands = {
    CmdProfile.class, 
    CmdBot.class, 
    CmdRender.class
})
public class FBSRCommandLineMain {
    public static void main(String[] args) {
        new CommandLine(new FBSRCommandLineMain());
    }
}
