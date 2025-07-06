package com.demod.fbsr.cli;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.app.RPCService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "blueprint", description = "Render images and other commands related to blueprint strings")
public class CmdBlueprint {

    @Command(name = "preview", description = "Render image with frame layout")
    public void previewRender(
            @Option(names = "-file", description = "Input blueprint string file") Optional<File> inputFile,
            @Parameters(description = "Input blueprint string") Optional<String> blueprintString
    ) {
        String input = blueprintString.orElse("");
        if (inputFile.isPresent()) {
            File file = inputFile.get();
            try {
                input = new String(Files.readAllBytes(file.toPath()));
            } catch (Exception e) {
                System.err.println("Error reading file: " + e.getMessage());
                return;
            }
        }

        Optional<String> response = RPCService.sendCommand("preview", input);
        if (response.isPresent()) {
            System.out.println("Response: " + response.get());
        } else {
            JSONObject ret = new JSONObject();
            ret.put("success", false);
            ret.put("message", "No response from blueprint bot service");
            System.err.println(ret.toString());
        }
    }

    @Command(name = "full", description = "Render full resolution image")
    public void fullRender(
            @Option(names = "-file", description = "Input blueprint string file") Optional<String> inputFile,
            @Option(names = "-options", description = "Additional rendering options (JSON)") Optional<String> options,
            @Parameters(description = "Input blueprint string") Optional<String> blueprintString
    ) {
        String input = blueprintString.orElse("");
        if (inputFile.isPresent()) {
            String filePath = inputFile.get();
            try {
                input = new String(Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            } catch (Exception e) {
                System.err.println("Error reading file: " + e.getMessage());
                return;
            }
        }

        Optional<String> response;
        if (options.isPresent()) {
            response = RPCService.sendCommand("full", input, options.get());
        } else {
            response = RPCService.sendCommand("full", input);
        }

        if (response.isPresent()) {
            System.out.println("Response: " + response.get());
        } else {
            JSONObject ret = new JSONObject();
            ret.put("success", false);
            ret.put("message", "No response from blueprint bot service");
            System.err.println(ret.toString());
        }
    }

}
