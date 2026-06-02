package com.runescape.web;

import com.runescape.Client;
import com.runescape.Configuration;
import com.runescape.io.jaggrab.JagGrabConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CheerpJClientLauncher {

    private static final String LOG_PREFIX = "[launcher] ";

    private CheerpJClientLauncher() {
    }

    public static void main(String[] args) {
        try {
            log("starting Mystic web launcher");
            Map<String, String> options = parseArgs(args);
            log("launcher args " + options);

            String server = options.get("server");
            if (server != null && !server.trim().isEmpty()) {
                Configuration.server_address = server.trim();
            }

            int serverPort = parseInt(options.get("port"), Configuration.server_port);
            int offset = parseInt(options.get("offset"), 0);
            Configuration.server_port = serverPort + offset;

            String bridgeBase = normalizeBridgeBase(options.get("bridge"));
            String gameBridge = options.getOrDefault("gameBridge", bridgeBase + "/game");
            String ondemandBridge = options.getOrDefault("ondemandBridge", bridgeBase + "/ondemand");

            log("server " + Configuration.server_address + ":" + Configuration.server_port);
            log("bridge routes game=" + gameBridge + " ondemand=" + ondemandBridge);
            Configuration.enableWebBridge(gameBridge, ondemandBridge, Configuration.server_port, JagGrabConstants.FILE_SERVER_PORT);

            syncBundledCache();

            log("launching applet-safe client startup");
            Client.launchWebClient();
        } catch (RuntimeException exception) {
            fail("fatal launcher error", exception);
            throw exception;
        } catch (Exception exception) {
            fail("fatal launcher error", exception);
            throw new IllegalStateException("Mystic web launcher failed", exception);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        if (args == null) {
            return result;
        }

        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            int separator = arg.indexOf('=');
            if (separator <= 0 || separator == arg.length() - 1) {
                continue;
            }
            String key = arg.substring(0, separator).trim();
            String value = arg.substring(separator + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalizeBridgeBase(String bridge) {
        String fallback = "/gamews";
        if (bridge == null || bridge.trim().isEmpty()) {
            return fallback;
        }
        String value = bridge.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? fallback : value;
    }

    private static void syncBundledCache() {
        Path sourceRoot = Paths.get("/app/cache");
        Path manifestPath = Paths.get("/app/cache-manifest.txt");
        Path targetRoot = Paths.get(Configuration.CACHE_DIRECTORY);
        if (!Files.exists(manifestPath)) {
            logError("bundled web cache manifest not found at " + manifestPath);
            return;
        }

        try {
            log("syncing bundled cache from " + sourceRoot + " to " + targetRoot);
            Files.createDirectories(targetRoot);
            List<String> relativePaths = Files.readAllLines(manifestPath);
            int copiedCount = 0;
            for (String relativePath : relativePaths) {
                if (relativePath == null || relativePath.trim().isEmpty()) {
                    continue;
                }
                copyCachePath(sourceRoot, targetRoot, sourceRoot.resolve(relativePath.trim()));
                copiedCount++;
            }
            log("bundled cache sync complete, files=" + copiedCount);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to sync bundled web cache", exception);
        }
    }

    private static void copyCachePath(Path sourceRoot, Path targetRoot, Path sourcePath) {
        Path relative = sourceRoot.relativize(sourcePath);
        Path targetPath = targetRoot.resolve(relative.toString());
        try {
            Files.createDirectories(targetPath.getParent());
            if (!Files.exists(targetPath) || Files.size(targetPath) != Files.size(sourcePath)) {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to copy cache file " + sourcePath + " to " + targetPath, exception);
        }
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }

    private static void logError(String message) {
        System.err.println(LOG_PREFIX + message);
    }

    private static void fail(String message, Exception exception) {
        System.err.println(LOG_PREFIX + message);
        exception.printStackTrace(System.err);
    }
}
