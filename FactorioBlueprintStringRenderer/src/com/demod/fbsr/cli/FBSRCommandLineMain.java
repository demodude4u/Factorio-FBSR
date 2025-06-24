package com.demod.fbsr.cli;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.demod.factorio.Config;
import com.demod.fbsr.Profile;

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
public class FBSRCommandLineMain {

    @Option(names = "-config", description = "Path to the configuration file (optional)", defaultValue = "config.json", scope = ScopeType.INHERIT)
    public void setConfigPath(File configPath) {
        Config.setPath(configPath.getAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        CommandLine cmd = new CommandLine(new FBSRCommandLineMain());

        if (args.length == 0) {
            System.out.println("No command provided. Starting interactive shell...");

            if (!Profile.vanilla().isValid()) {
                System.out.println();
                System.out.println("WARNING: The vanilla profile is missing or not valid! Type command 'profile default-vanilla' to get started.");
            
            } else if (!Profile.listProfiles().stream().allMatch(Profile::isReady)) {
                System.out.println();
                System.out.println("WARNING: Not all profiles are ready!");
                new CmdProfile().listProfiles(null, false);
            }

            System.out.println();
            System.out.println("Type 'help profile' for a list of commands to create, manage, build, clear, or delete profiles.");
            System.out.println("Type 'help bot' for a list of commands to run the bot or start/stop bot service.");
            System.out.println("Type 'help render' for a list of commands to render images via CLI.");
            System.out.println("Type 'help lua' for a list of commands to access factorio data via CLI.");
            interactiveShell(cmd);
        } else {
            System.exit(cmd.execute(args));
        }
    }

    private static void interactiveShell(CommandLine cmd) throws IOException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();

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

}
