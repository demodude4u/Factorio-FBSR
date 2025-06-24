package com.demod.fbsr.cli;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.demod.factorio.Config;
import com.demod.fbsr.Profile;
import com.google.common.util.concurrent.Uninterruptibles;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.shell.jline3.PicocliCommands;

@Command(name = "", mixinStandardHelpOptions = true, subcommands = {
    CommandLine.HelpCommand.class,
    CmdProfile.class, 
    CmdBot.class, 
    CmdRender.class,
    CmdDump.class
})
public class FBSRCommands {

    @Option(names = "-config", description = "Path to the configuration file (optional)", defaultValue = "config.json", scope = ScopeType.INHERIT)
    public void setConfigPath(File configPath) {
        Config.setPath(configPath.getAbsolutePath());
    }

    public static void interactiveShell() {
        System.out.println(" _____ _____ _____ _____ ");
        System.out.println("|   __| __  |   __| __  |");
        System.out.println("|   __| __ -|__   |    -|");
        System.out.println("|__|  |_____|_____|__|__|");
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        System.out.println();
        System.out.println("Type 'help profile' for a list of commands to create, manage, build, clear, or delete profiles.");
        System.out.println("Type 'help bot' for a list of commands to run the bot or start/stop bot service.");
        System.out.println("Type 'help render' for a list of commands to render images via CLI.");
        System.out.println("Type 'help lua' for a list of commands to access factorio data via CLI.");
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        
        System.out.println();
        System.out.println("If profiles are new or not built, run command 'profile build -all'");
        System.out.println("If a profile.json configuration was changed, run command 'profile build <PROFILE> -force'");
        System.out.println("If a mod was updated, run command 'profile build <PROFILE> -force'");
        System.out.println("If factorio was updated, run command 'profile build -all -force-dump'");
        System.out.println("If FBSR was updated, run command 'profile build -all -force-data'");
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        System.out.println();
        System.out.println("Starting interactive shell...");

        if (!Profile.vanilla().isValid()) {
            System.out.println();
            System.out.println("WARNING: The vanilla profile is missing or not valid! Type command 'profile default-vanilla' to get started.");
        
        } else if (!Profile.listProfiles().stream().allMatch(Profile::isReady)) {
            System.out.println();
            System.out.println("WARNING: Not all profiles are ready!");
            new CmdProfile().listProfiles(null, false);
        }
        System.out.println();
        System.out.println("Type 'exit' or 'quit' to exit.");

        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder().system(true).build();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        CommandLine cmd = new CommandLine(new FBSRCommands());
        PicocliCommands picocliCommands = new PicocliCommands(cmd);

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                // .completer(picocliCommands.compileCompleters()) //TODO crashes
                .parser(new DefaultParser())
                .variable(LineReader.LIST_MAX, 50)
                .build();

        String prompt = "\nFBSR> ";

        while (true) {
            String line;
            try {
                line = reader.readLine(prompt);
                if (line == null || line.trim().equalsIgnoreCase("exit") || line.trim().equalsIgnoreCase("quit")) break;
                cmd.execute(line.split(" "));
            } catch (UserInterruptException | EndOfFileException e) {
                break;
            } catch (Exception ex) {
                System.err.println("Error: ");
                ex.printStackTrace();
            }
        }

        System.out.println("Goodbye.");
    }

    public static void execute(String[] args) throws IOException {
        CommandLine cmd = new CommandLine(new FBSRCommands());
        System.exit(cmd.execute(args));
    }
}
