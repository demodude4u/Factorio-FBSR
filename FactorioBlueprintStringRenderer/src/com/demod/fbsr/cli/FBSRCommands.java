package com.demod.fbsr.cli;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;
import com.google.common.util.concurrent.Uninterruptibles;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.TypeConversionException;
import picocli.shell.jline3.PicocliCommands;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Model.ArgSpec;

@Command(name = "", mixinStandardHelpOptions = true, subcommands = {
    CommandLine.HelpCommand.class,
    CmdConfig.class,
    CmdProfile.class, 
    CmdBlueprint.class,
    CmdFactorio.class,
    CmdBot.class,
    FBSRCommands.DumpHelpCommand.class
})
public class FBSRCommands {

    // @Option(names = "-config", description = "Path to the configuration file (optional)", defaultValue = "config.json", scope = ScopeType.INHERIT)
    // public void setConfigPath(File configPath) {
    //     Config.setPath(configPath.getAbsolutePath());
    // }

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

        CommandLine cmd = createCommandLine();

        File fileConfig = new File(Config.getPath());
        if (!fileConfig.exists()) {
            System.out.println();
            System.out.println("The configuration file is missing! Creating a new config file.");
            InputStream template = FBSRCommands.class.getClassLoader().getResourceAsStream("config-template.json");
            try {
                Files.copy(template, fileConfig.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            System.out.println("File created: " + fileConfig.getAbsolutePath());

            // Run initial setup commands
            cmd.execute("dump-help");
            cmd.execute("config","find-factorio");
            cmd.execute("profile","default-vanilla");
        }

        if (FactorioManager.hasFactorioInstall()) {
            System.out.println();
            System.out.println("Factorio installed: Version " + FactorioManager.getFactorioVersion());
        } else {
            System.out.println();
            System.out.println("Factorio is not installed. Type `help config factorio` to learn how to configure it, or type `config find-factorio` to find the installation.");
        }

        if (!Profile.vanilla().isValid()) {
            System.out.println();
            System.out.println("WARNING: The vanilla profile is missing or not valid! Type command 'profile default-vanilla' to get started.");
        
        } else if (!Profile.listProfiles().stream().allMatch(p -> !p.isEnabled() || p.isReady())) {
            System.out.println();
            System.out.println("WARNING: Not all profiles are ready! Type 'profile build -all' to build all profiles.");
            new CmdProfile().listProfiles(null, false);

        } else {
            System.out.println();
            System.out.println("All profiles are ready! Type command 'bot run' to start the bot or 'render preview <STRING>' to render an image.");
        }

        System.out.println();
        System.out.println("Type 'exit' or 'quit' or ctrl-c to exit.");

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
                .variable(LineReader.HISTORY_FILE, "fbsrcli.history")
                .variable(LineReader.HISTORY_FILE_SIZE, 100)
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

        try {
            reader.getHistory().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Goodbye.");
    }

    public static void execute(String[] args) throws IOException {
        File fileConfig = new File(Config.getPath());
        if (!fileConfig.exists()) {
            System.err.println("Configuration file not found: " + fileConfig.getAbsolutePath());
            System.err.println("Please run in interactive mode (no arguments) to create a new configuration file.");
            System.exit(1);
        }

        CommandLine cmd = createCommandLine();
        System.exit(cmd.execute(args));
    }

    public static CommandLine createCommandLine() {
        CommandLine cmd = new CommandLine(new FBSRCommands());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.registerConverter(Color.class, new ColorConverter());
        return cmd;
    }

    @Command(name = "dump-help", description = "Dump all command help into a Markdown file")
    public static class DumpHelpCommand implements Runnable {
        @Option(names = {"-o", "--output"}, description = "Output Markdown file", defaultValue = "fbsr-cli-help.md")
        private String outputFile;

        @Override
        public void run() {
            try {
                CommandLine rootCmd = new CommandLine(new FBSRCommands());
                StringBuilder sb = new StringBuilder();
                dumpHelpRecursive(rootCmd, sb, 1, "");
                Path outPath = Path.of(outputFile);
                try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
                    pw.print(sb.toString());
                }
                System.out.println("Help dumped to: " + outPath.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Failed to dump help: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void dumpHelpRecursive(CommandLine cmd, StringBuilder sb, int level, String parentPath) {
            CommandSpec spec = cmd.getCommandSpec();
            String cmdName = spec.name();
            String fullPath = parentPath.isEmpty() ? cmdName : parentPath + " " + cmdName;
            String headerPrefix = "#".repeat(Math.min(level, 6));
            boolean topHeader = (level == 1);
            if (topHeader) {
                sb.append("# FBSR CLI Commands\n\n");
                sb.append("_(This document is auto-generated by running `dump-help`)_\n\n");
            } else {
                sb.append(headerPrefix).append(" `").append(fullPath.trim()).append("`");
            }
            if (spec.usageMessage().description().length > 0) {
                sb.append(" — ").append(String.join(" ", spec.usageMessage().description()));
            }
            sb.append("\n\n");

            // Usage
            sb.append("```shell\n");
            sb.append(cmd.getUsageMessage(CommandLine.Help.Ansi.OFF));
            sb.append("\n```\n");

            // List subcommands
            // if (!spec.subcommands().isEmpty()) {
            //     sb.append("**Subcommands:**\n\n");
            //     for (Map.Entry<String, CommandLine> entry : spec.subcommands().entrySet()) {
            //         CommandSpec subSpec = entry.getValue().getCommandSpec();
            //         if (!subSpec.usageMessage().hidden()) {
            //             sb.append("- `").append(entry.getKey()).append("`");
            //             if (subSpec.usageMessage().description().length > 0) {
            //                 sb.append(": ").append(String.join(" ", subSpec.usageMessage().description()));
            //             }
            //             sb.append("\n");
            //         }
            //     }
            //     sb.append("\n");
            // }

            for (Map.Entry<String, CommandLine> entry : spec.subcommands().entrySet()) {
                CommandSpec subSpec = entry.getValue().getCommandSpec();
                if (!subSpec.usageMessage().hidden()) {
                    dumpHelpRecursive(entry.getValue(), sb, level + 1, fullPath);
                }
            }
        }
    }

    public static class ColorConverter implements ITypeConverter<Color> {
        @Override
        public Color convert(String value) throws Exception {
            String hex = value.trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            try {
                if (hex.length() == 8) {
                    // AARRGGBB
                    int val = (int) Long.parseLong(hex, 16);
                    return new Color(val, true);
                } else if (hex.length() == 6) {
                    // RRGGBB
                    int val = Integer.parseInt(hex, 16);
                    return new Color(val, false);
                } else {
                    throw new TypeConversionException("Color hex must be 6 (RRGGBB) or 8 (AARRGGBB) hex digits: " + value);
                }
            } catch (NumberFormatException e) {
                throw new TypeConversionException("Invalid color hex: " + value);
            }
        }
    }
}
