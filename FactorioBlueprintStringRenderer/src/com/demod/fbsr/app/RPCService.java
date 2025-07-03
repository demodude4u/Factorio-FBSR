package com.demod.fbsr.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Config;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.cli.CmdRPC;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Uninterruptibles;

import picocli.CommandLine;

public class RPCService extends AbstractIdleService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RPCService.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private int port;
    private ServerSocket serverSocket;

    @Override
    protected void startUp() throws Exception {
        port = getPort();

        if (sendCommand("kill").isPresent()) {// Ensure any previous instance is killed
            LOGGER.info("Previous RPC service instance found, sending kill command...");
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
            while (true) {
                try {
                    serverSocket = new ServerSocket(port);
                } catch (IOException e) {
                    LOGGER.info("Port {} is still in use, waiting for it to be released...", port);
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                    continue;
                }
                break;
            }

        } else {
            serverSocket = new ServerSocket(port);
        }

        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    LOGGER.debug("Accepted RPC connection from {}", socket.getRemoteSocketAddress());
                    executor.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        LOGGER.error("Error handling RPC connection", e);
                    }
                }
            }
        });
        LOGGER.info("RPC service started on port {}", port);
    }

    private void handleConnection(Socket socket) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            String inputLine = reader.readLine();
            LOGGER.debug("Received RPC command: {}", inputLine);
            if (inputLine == null || inputLine.isBlank()) return;

            List<String> args = new ArrayList<>(new JSONArray(inputLine).toList().stream().map(Object::toString).collect(Collectors.toList()));

            boolean error = false;
            String errorMessage = null;
            Object result = null;
            
            if (args.isEmpty()) {
                error = true;
                errorMessage = "No command provided.";
            } else {
                String command = args.remove(0);
                Optional<Method> commandMethod = CommandLine.getCommandMethods(CmdRPC.class, command).stream().findFirst();
                if (!commandMethod.isEmpty()) {
                    CommandLine commandLine = new CommandLine(commandMethod.get());
                    commandLine.execute(args.toArray(String[]::new));
                    result = commandLine.getExecutionResult();
                } else {
                    error = true;
                    errorMessage = "Unknown command: " + command;
                }
            }

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("error", error);

            if (error) {
                LOGGER.error("Error executing RPC command: {}", errorMessage);
                jsonResponse.put("errorMessage", errorMessage);

            } else {
                LOGGER.debug("RPC command result: {}", result.toString());
                jsonResponse.put("result", result);
            }

            writer.write(jsonResponse.toString());
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Error handling RPC connection", e);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    @Override
    protected void shutDown() throws Exception {
        serverSocket.close();
        executor.shutdownNow();
    }

    public static int getPort() {
        JSONObject config = Config.get().optJSONObject("rpc", new JSONObject());
        return config.optInt("port", 50832);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> sendCommand(String... args) {
        int port = getPort();

        try (Socket socket = new Socket("localhost", port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JSONArray jsonArgs = new JSONArray();
            for (String arg : args) {
                jsonArgs.put(arg);
            }
            writer.write(jsonArgs.toString() + "\n");
            writer.flush();

            try {
                JSONObject jsonResponse = new JSONObject(reader.readLine());
                if (jsonResponse.getBoolean("error")) {
                    System.out.println("RPC Error: " + jsonResponse.getString("errorMessage"));
                    return Optional.empty();
                }
                return Optional.of((T) jsonResponse.get("result"));
            } catch (Exception e) {
                System.out.println("Error parsing RPC response: " + e.getMessage());
                return Optional.empty();
            }
        } catch (ConnectException e) {
            return Optional.empty();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
