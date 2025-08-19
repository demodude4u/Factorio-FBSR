package com.demod.fbsr.cli;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.json.JSONObject;
import org.rapidoid.annotation.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.Utils;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.BlueprintFinder.FindBlueprintResult;
import com.demod.fbsr.WebUtils;
import com.demod.fbsr.app.DiscordService;
import com.demod.fbsr.app.RPCService;
import com.demod.fbsr.app.ServiceFinder;
import com.demod.fbsr.app.FBSRApps;
import com.demod.fbsr.bs.BSBlueprintString;
import com.demod.fbsr.gui.layout.GUILayoutBlueprint;
import com.demod.fbsr.gui.layout.GUILayoutBook;
import com.fasterxml.jackson.databind.JsonSerializable.Base;
import com.google.common.util.concurrent.Uninterruptibles;

import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "")
public class CmdRPC {
	private static final Logger LOGGER = LoggerFactory.getLogger(CmdRPC.class);

    public static final long START_STAMP = System.currentTimeMillis();

    private volatile boolean killRequested = false;

    @Command(name = "ping")
    public long ping() {
        return System.currentTimeMillis() - START_STAMP;
    }

    @Command(name = "status")
    public String status() {
        return FBSRApps.status;
    }

    @Command(name = "kill")
    public boolean kill() {
        if (killRequested) {
            return false;
        }
        killRequested = true;
        LOGGER.info("Kill command received, shutting down...");
        new Thread(() -> {
            Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Goodbye!");
            System.exit(0);
        }).start();
        return true;
    }

    @Command(name = "preview")
    public JSONObject preview(
            @Parameters(arity = "1") String blueprintStringRaw
    ) {
        JSONObject ret = new JSONObject();
        CommandReporting reporting = new CommandReporting(null, null, "preview " + blueprintStringRaw, Instant.now());
        try {
            List<FindBlueprintResult> search = BlueprintFinder.search(blueprintStringRaw);
            
            if (search.isEmpty()) {
                ret.put("success", false);
                ret.put("message", "No blueprint found for the given string");
                reportAddResponse(reporting, ret);
                return ret;
            }
            
            FindBlueprintResult searchResult = search.get(0);
            if (searchResult.failureCause.isPresent()) {
                ret.put("success", false);
                ret.put("message", "Error while parsing blueprint");
                ret.put("reason", searchResult.failureCause.get().getMessage());
                reportAddResponse(reporting, ret);
                return ret;
            }

            BSBlueprintString blueprintString = searchResult.blueprintString.get();
            
            String type;
            BufferedImage image;
            if (blueprintString.blueprint.isPresent()) {
                type = "blueprint";
                GUILayoutBlueprint layout = new GUILayoutBlueprint();
                layout.setBlueprint(blueprintString.blueprint.get());
                layout.setReporting(reporting);
                image = layout.generateDiscordImage();

            } else if (blueprintString.blueprintBook.isPresent()) {
                type = "book";
                GUILayoutBook layout = new GUILayoutBook();
                layout.setBook(blueprintString.blueprintBook.get());
                layout.setReporting(reporting);
                image = layout.generateDiscordImage();

            } else {
                ret.put("success", false);
                ret.put("message", "Blueprint string is not a blueprint or blueprint book");
                reportAddResponse(reporting, ret);
                return ret;
            }

            ret.put("success", true);
            ret.put("type", type);
            Optional<String> firstLabel = blueprintString.findFirstLabel();
            if (firstLabel.isPresent()) {
                ret.put("label", firstLabel.get());
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", baos);
                baos.flush();
                ret.put("filename", WebUtils.formatBlueprintFilename(blueprintString.findFirstLabel(), "png"));
                ret.put("filedata", Base64.getEncoder().encodeToString(baos.toByteArray()));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            
            reportAddResponse(reporting, ret);
            return ret;
        } finally {
            ServiceFinder.findService(DiscordService.class).ifPresent(s -> s.getBot().submitReport(reporting));
        }
    }

    @Command(name = "request")
    public JSONObject request(
            @Parameters(arity = "1") String blueprintStringRaw,
            @Parameters(arity = "0..1") Optional<String> options
    ) {
        JSONObject ret = new JSONObject();
        CommandReporting reporting = new CommandReporting(null, null, "request " + blueprintStringRaw, Instant.now());
        
        try {
            Optional<JSONObject> jsonOptions;
            if (options.isPresent()) {
                try {
                    jsonOptions = Optional.of(new JSONObject(options.get()));
                    reporting.addField(new Field("Options", jsonOptions.get().toString(2), false));
                } catch (Exception e) {
                    reporting.addField(new Field("Options (Invalid)", options.get(), false));
                    ret.put("success", false);
                    ret.put("message", "Invalid options JSON");
                    ret.put("reason", e.getMessage());
                    reportAddResponse(reporting, ret);
                    return ret;
                }
            } else {
                jsonOptions = Optional.empty();
            }

            List<FindBlueprintResult> search = BlueprintFinder.search(blueprintStringRaw);
            
            if (search.isEmpty()) {
                ret.put("success", false);
                ret.put("message", "No blueprint found for the given string");
                reportAddResponse(reporting, ret);
                return ret;
            }
            
            FindBlueprintResult searchResult = search.get(0);
            if (searchResult.failureCause.isPresent()) {
                ret.put("success", false);
                ret.put("message", "Error while parsing blueprint");
                ret.put("reason", searchResult.failureCause.get().getMessage());
                reportAddResponse(reporting, ret);
                return ret;
            }

            BSBlueprintString blueprintString = searchResult.blueprintString.get();
            
            BufferedImage image;
            if (blueprintString.blueprint.isPresent()) {
                RenderRequest request;
                if (jsonOptions.isPresent()) {
                    request = new RenderRequest(blueprintString.blueprint.get(), reporting, jsonOptions.get());
                } else {
                    request = new RenderRequest(blueprintString.blueprint.get(), reporting);
                }

                RenderResult result = null;
                try {
                    result = FBSR.renderBlueprintAsync(request).get();
                } catch (InterruptedException | ExecutionException e) {
                    ret.put("success", false);
                    ret.put("message", "Internal failure: " + e.getMessage());
                    reportAddResponse(reporting, ret);
                    return ret;
                }
                
                image = result.image;

            } else {
                ret.put("success", false);
                ret.put("message", "Blueprint string is not a single blueprint");
                reportAddResponse(reporting, ret);
                return ret;
            }

            ret.put("success", true);
            Optional<String> firstLabel = blueprintString.findFirstLabel();
            if (firstLabel.isPresent()) {
                ret.put("label", firstLabel.get());
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", baos);
                baos.flush();
                ret.put("filename", WebUtils.formatBlueprintFilename(blueprintString.findFirstLabel(), "png"));
                ret.put("filedata", Base64.getEncoder().encodeToString(baos.toByteArray()));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            
            reportAddResponse(reporting, ret);
            return ret;
        } finally {
            ServiceFinder.findService(DiscordService.class).ifPresent(s -> s.getBot().submitReport(reporting));
        }
    }

    private void reportAddResponse(CommandReporting reporting, JSONObject ret) {
        JSONObject retCopy = new JSONObject(ret.toString());
        String fileData = (String)retCopy.remove("filedata");
        if (fileData != null) {
            retCopy.put("filedata", "... (" + fileData.length() + " characters)");
        }
        reporting.addField(new Field("Response", retCopy.toString(2), false));
    }

}
