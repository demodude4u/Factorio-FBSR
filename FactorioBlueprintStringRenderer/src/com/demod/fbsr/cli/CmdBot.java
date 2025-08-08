package com.demod.fbsr.cli;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.rapidoid.annotation.Param;
import org.rapidoid.data.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.Profile;
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.app.RPCService;
import com.demod.fbsr.bs.BSColor;
import com.demod.fbsr.app.FBSRApps;
import com.google.common.util.concurrent.Uninterruptibles;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = " ", description = "Blueprint Bot commands")
public class CmdBot {
	private static final Logger LOGGER = LoggerFactory.getLogger(CmdBot.class);

    @Command(name = "bot-run", description = "Run Blueprint Bot")
    public static void runBot(
        @Parameters(arity = "0..*", description = "Profiles to be loaded", paramLabel = "<PROFILE>") List<String> requestedProfiles,
        @Option(names = "-ignore-not-ready", description = "Ignore profiles that are not ready", defaultValue = "false") boolean ignoreNotReady,
        @Option(names = "-require-all-enabled", description = "Require all profiles to be enabled and ready before starting the bot", defaultValue = "false") boolean requireAllEnabled
    ) {
        if (!ignoreNotReady && !checkProfilesReady(requireAllEnabled)) {
            return;
        }

        if (!FBSRApps.start(requestedProfiles)) {
            LOGGER.error("Failed to start Blueprint Bot service. Please check the configuration and try again.");
            return;
        }

        if (!FBSRApps.waitForStopped(true)) {
            LOGGER.error("Unexpected error occurred while waiting for Blueprint Bot service to stop.");
        }
    }

    @Command(name = "bot-kill", description = "Stop Blueprint Bot service")
    public static void killBot() {
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

    @Command(name = "bot-status", description = "Get the status of Blueprint Bot service")
    public static void status() {
        String status = RPCService.<String>sendCommand("status").orElse("dead");
        LOGGER.info("Blueprint Bot service status: {}", status);
    }

    private static boolean checkProfilesReady(boolean requireAllEnabled) {
        if (requireAllEnabled && !Profile.listProfiles().stream().allMatch(p -> p.isEnabled())) {
            System.out.println("Some profiles are not enabled. Please ensure all profiles are valid and have been built.");
            for (Profile profile : Profile.listProfiles()) {
                System.out.println("Profile: " + profile.getName() + " (" + profile.getStatus() + ")");
            }
            return false;
        }

        List<Profile> enabledProfiles = Profile.listProfiles().stream().filter(Profile::isEnabled).collect(Collectors.toList());

        if (!enabledProfiles.stream().allMatch(p->p.getStatus() == ProfileStatus.READY)) {
            System.out.println("Some profiles are not ready. Please ensure all profiles are valid and have been built. Add `-ignore-not-ready` to ignore this check, or type `profile-disable <PROFILE>` to disable a profile.");
            for (Profile profile : Profile.listProfiles()) {
                System.out.println("Profile: " + profile.getName() + " (" + profile.getStatus() + ")");
            }
            return false;
        }
        
        return true;
    }

    private static class RenderInput {
        @Parameters(arity = "1", description = "Blueprint string", paramLabel = "<STRING>") Optional<String> blueprintString;
        @Option(names = "-input-file", description = "Blueprint file", paramLabel = "<PATH>") Optional<File> blueprintFile;
        @Option(names = "-url", description = "Blueprint URL", paramLabel = "<URL>") Optional<URL> blueprintUrl;

        public Optional<String> read() {
            if (blueprintString.isPresent()) {
                return blueprintString;
            
            } else if (blueprintUrl.isPresent()) {
                return blueprintUrl.map(URL::toString);
            
            } else if (blueprintFile.isPresent()) {
                try {
                    return Optional.of(Files.readString(blueprintFile.get().toPath()));
                } catch (IOException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }

    private static class RenderOutput {
        @Option(names = "-test", description = "Save to test file and open it") boolean test;
        @Option(names = "-json", description = {"Output as base64 string encapsulated in JSON format:",
                                                "{\"success\": true", 
                                                "  \"label\": \"Blueprint Label\",    (optional)",
                                                "  \"filename\": \"blueprint.png\",",
                                                "  \"filedata\": \"base64string\"}",
                                                "    OR    ",
                                                "{\"success\": false, \"message\": \"error message\"}"}) boolean json;
        @Option(names = "-output-file", description = {"Save to file with reply in JSON format:",
                                                "{\"success\": true}",
                                                "    OR    ",
                                                "{\"success\": false, \"message\": \"error message\"}"}, paramLabel = "<PATH>") Optional<File> file;
    
        public boolean write(JSONObject jsonResponse) {
            if (!jsonResponse.has("success")) {
                replyError("JSON response does not contain 'success' field.");
                return false;
            }

            if (!jsonResponse.getBoolean("success")) {
                System.out.println(jsonResponse.toString(2));
                return false;
            }

            if (json) {
                System.out.println(jsonResponse.toString(2));
                return true;
            }

            File fileOut;
            if (test) {
                File folderTest = new File("test");
                folderTest.mkdirs();
                
                if (!jsonResponse.has("filename")) {
                    replyError("JSON response does not contain 'filename'.");
                    return false;
                }
                
                fileOut = new File(folderTest, jsonResponse.getString("filename"));                
            
            } else if (file.isPresent()) {
                fileOut = file.get();
            
            } else {
                replyError("No output specified.");
                return false;
            }

            if (!jsonResponse.has("filedata")) {
                replyError("JSON response does not contain 'filedata'.");
                return false;
            }

            String base64Data = jsonResponse.getString("filedata");
            jsonResponse.remove("filedata");
            try {
                byte[] data = Base64.getDecoder().decode(base64Data);
                Files.write(fileOut.toPath(), data);
                System.out.println(jsonResponse.toString(2));

                if (test) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(fileOut);
                    }
                }

                return true;
            } catch (IOException e) {
                replyError("Error writing file: " + e.getMessage());
                return false;
            }
        }

        public void replyError(String message) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", message);
            System.out.println(errorResponse.toString(2));
        }
    }

    public static class RenderMode {
        @Option(names = "-full", description = "Render full resolution image (blueprints only, no books)") boolean full;
        @Option(names = "-preview", description = "Render preview image") boolean preview;
    }

    public static enum RenderFormat {
        png, jpg, webp
    }

    @Command(name = "bot-render", description = "Render a blueprint using the bot RPC service")
    public static void renderBlueprint(
        @ArgGroup(exclusive = true, multiplicity = "1") RenderInput input,
        @ArgGroup(exclusive = true, multiplicity = "1") RenderOutput output,
        @ArgGroup(exclusive = true, multiplicity = "1") RenderMode mode
    ) {

        Optional<String> blueprintStringRaw = input.read();
        if (blueprintStringRaw.isEmpty()) {
            output.replyError("Unable to process blueprint string.");
            return;
        }

        Optional<JSONObject> jsonResponse;
        if (mode.preview) {
            jsonResponse = RPCService.<JSONObject>sendCommand("preview", blueprintStringRaw.get());

        } else if (mode.full) {
            jsonResponse = RPCService.<JSONObject>sendCommand("request", blueprintStringRaw.get());

        } else {
            output.replyError("No render mode specified. Use -full or -preview.");
            return;
        }

        if (jsonResponse.isEmpty()) {
            output.replyError("No valid response from Blueprint Bot service.");
            return;
        }

        output.write(jsonResponse.get());
    }

    @Command(name = "bot-render-custom", description = "Render a custom blueprint using the bot RPC service")
    public static void renderCustomBlueprint(
        @ArgGroup(exclusive = true, multiplicity = "1") RenderInput input,
        @ArgGroup(exclusive = true, multiplicity = "1") RenderOutput output,
        @Option(names = "-format", description = "Render image format (${COMPLETION-CANDIDATES})", paramLabel = "<FORMAT>") RenderFormat format,
        @Option(names = "-min-width", description = "Minimum width in pixels", paramLabel = "<PIXELS>") Optional<Integer> minWidth,
        @Option(names = "-min-height", description = "Minimum height in pixels", paramLabel = "<PIXELS>") Optional<Integer> minHeight,
        @Option(names = "-max-width", description = "Maximum width in pixels", paramLabel = "<PIXELS>") Optional<Integer> maxWidth,
        @Option(names = "-max-height", description = "Maximum height in pixels", paramLabel = "<PIXELS>") Optional<Integer> maxHeight,
        @Option(names = "-max-scale", description = "Maximum scale of the blueprint entities", paramLabel = "<SCALE>") Optional<Double> maxScale,
        @Option(names = "-clip-sprites", description = "Clip sprites that are outside the blueprint area") boolean clipSprites,
        @Option(names = "-dont-clip-sprites", description = "Do not clip sprites that are outside the blueprint area") boolean dontClipSprites,
        @Option(names = "-show-background", description = "Show background behind blueprint") boolean showBackground,
        @Option(names = "-hide-background", description = "Transparent background behind blueprint") boolean hideBackground,
        @Option(names = "-background-color", description = "Background color in hex format (e.g. #RRGGBB or #AARRGGBB)", paramLabel = "<COLOR>") Optional<Color> background,
        @Option(names = "-show-gridlines", description = "Show grid lines in the blueprint") boolean showGridLines,
        @Option(names = "-hide-gridlines", description = "Do not grid lines in the blueprint") boolean hideGridLines,
        @Option(names = "-gridlines-color", description = "Grid lines color in hex format (e.g. #RRGGBB or #AARRGGBB)", paramLabel = "<COLOR>") Optional<Color> gridLinesColor,
        @Option(names = "-show-alt-mode", description = "Show alt mode icons and symbols") boolean showAltMode,
        @Option(names = "-hide-alt-mode", description = "Hide alt mode icons and symbols") boolean hideAltMode,
        @Option(names = "-show-grid-numbers", description = "Show grid numbers in the blueprint") boolean showGridNumbers,
        @Option(names = "-hide-grid-numbers", description = "Hide grid numbers in the blueprint") boolean hideGridNumbers
    ) {

        Optional<String> blueprintStringRaw = input.read();
        if (blueprintStringRaw.isEmpty()) {
            output.replyError("Unable to process blueprint string.");
            return;
        }

        JSONObject options = new JSONObject();
        maxWidth.ifPresent(w -> options.put("max_width", w));
        maxHeight.ifPresent(h -> options.put("max_height", h));
        minWidth.ifPresent(w -> options.put("min_width", w));
        minHeight.ifPresent(h -> options.put("min_height", h));
        maxScale.ifPresent(s -> options.put("max_scale", s));
        if (dontClipSprites) {
            options.put("dont_clip_sprites", true);
        }
        if (clipSprites) {
            options.put("dont_clip_sprites", false);
        }
        if (showBackground) {
            options.put("show_background", true);
        }
        if (hideBackground) {
            options.put("show_background", false);
        }
        background.ifPresent(c -> options.put("background", BSColor.toJson(c)));
        if (showGridLines) {
            options.put("show_gridlines", true);
        }
        if (hideGridLines) {
            options.put("show_gridlines", false);
        }
        gridLinesColor.ifPresent(c -> options.put("gridlines_color", BSColor.toJson(c)));

        Optional<JSONObject> jsonResponse = RPCService.<JSONObject>sendCommand("request", blueprintStringRaw.get(), options.toString());

        if (jsonResponse.isEmpty()) {
            output.replyError("No valid response from Blueprint Bot service.");
            return;
        }

        output.write(jsonResponse.get());
    }
}
