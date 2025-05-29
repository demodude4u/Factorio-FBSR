package com.demod.fbsr.cli;

import java.util.Optional;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "render", description = "Render images from blueprint strings")
public class CmdRender {

    @Command(name = "preview", description = "Render image with frame layout")
    public void previewRender(
            @Option(names = "-file", description = "Input blueprint string file", required = true) Optional<String> inputFile,
            @Parameters(description = "Input blueprint string") Optional<String> blueprintString
    ) {
        // TODO
    }

    @Command(name = "full", description = "Render full resolution image")
    public void fullRender(
            @Option(names = "-file", description = "Input blueprint string file", required = true) Optional<String> inputFile,
            @Parameters(description = "Input blueprint string") Optional<String> blueprintString
    ) {
        // TODO
    }

}
