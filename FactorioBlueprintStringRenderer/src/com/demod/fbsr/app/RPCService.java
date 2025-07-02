package com.demod.fbsr.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private CommandLine commandLine;
    private ServerSocket serverSocket;

    @Override
    protected void startUp() throws Exception {
        
        ServiceFinder.findService(FactorioService.class).get().awaitRunning();

        port = getPort();
        commandLine = new CommandLine(new CmdRPC());

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
                    executor.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
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
            if (inputLine == null || inputLine.isBlank()) return;

            String[] args = new JSONArray(inputLine).toList().toArray(String[]::new);
            commandLine.execute(args);
            Object ret = commandLine.getExecutionResult();
            if (ret != null) {
                writer.write(ret.toString());
            }
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
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

    public static Optional<String> sendCommand(String... args) {
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

            return Optional.of(reader.readLine());
        } catch (ConnectException e) {
            return Optional.empty();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
