package com.osroyale;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Handles Discord OAuth2 local callback listener and URL authorization.
 */
public class DiscordOAuth {

    private static final long BROWSER_OPEN_DELAY_MS = 5000L;

    private static DiscordOAuth instance;
    private HttpServer httpServer;
    private Consumer<String> onCodeReceived;
    private TimerTask pendingBrowserOpen;

    public static DiscordOAuth getInstance() {
        if (instance == null) {
            instance = new DiscordOAuth();
        }
        return instance;
    }

    public static String getOAuthUrl() {
        try {
            String clientId = Configuration.DiscordConfiguration.CLIENT_ID;
            String redirectUri = URLEncoder.encode(Configuration.DiscordConfiguration.REDIRECT_URL, StandardCharsets.UTF_8.name());
            return "https://discord.com/api/oauth2/authorize?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code&scope=identify%20email";
        } catch (Exception e) {
            return "https://discord.com/api/oauth2/authorize?client_id=" + Configuration.DiscordConfiguration.CLIENT_ID + "&redirect_uri=" + Configuration.DiscordConfiguration.REDIRECT_URL + "&response_type=code&scope=identify%20email";
        }
    }

    public DiscordOAuth() {
        int initialPort = Configuration.DiscordConfiguration.CALLBACK_PORT;
        for (int p = initialPort; p < initialPort + 10; p++) {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(p), 0);
                CallbackHandler handler = new CallbackHandler();
                httpServer.createContext("/", handler);
                httpServer.createContext("/discord/callback", handler);
                httpServer.setExecutor(null);
                httpServer.start();
                System.out.println("[DiscordOAuth] Local callback server listening on http://localhost:" + p);
                break;
            } catch (Exception e) {
                System.out.println("[DiscordOAuth] Port " + p + " busy, trying next port...");
            }
        }
    }

    public void startAuthFlow(Consumer<String> codeCallback) {
        this.onCodeReceived = codeCallback;
        cancelPendingBrowserOpen();
        String url = getOAuthUrl();
        pendingBrowserOpen = new TimerTask() {
            @Override
            public void run() {
                System.out.println("[DiscordOAuth] Opening browser for authorization: " + url);
                openBrowser(url);
            }
        };
        new Timer("discord-oauth-browser-delay", true).schedule(pendingBrowserOpen, BROWSER_OPEN_DELAY_MS);
    }

    /**
     * Cancels a browser open still waiting out its delay, and drops the pending code callback.
     * Called when the player backs out of account creation before the delay elapses.
     */
    public void cancelAuthFlow() {
        cancelPendingBrowserOpen();
        onCodeReceived = null;
    }

    private void cancelPendingBrowserOpen() {
        if (pendingBrowserOpen != null) {
            pendingBrowserOpen.cancel();
            pendingBrowserOpen = null;
        }
    }

    private class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String code = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "code".equals(pair[0])) {
                        code = pair[1];
                        break;
                    }
                }
            }
            System.out.println("[DiscordOAuth] Callback received: path=" + exchange.getRequestURI().getPath()
                    + " query=" + query
                    + " code=" + (code == null ? "MISSING" : "present(len=" + code.length() + ")")
                    + " onCodeReceived=" + (onCodeReceived == null ? "null (already consumed or never started)" : "set"));

            String htmlResponse = "<html><body style='background:#1e1e2e;color:#5865F2;font-family:sans-serif;text-align:center;padding-top:60px;'>"
                    + "<h1>Discord Authorization Successful!</h1>"
                    + "<p style='color:#ffffff;'>You can now return to the game client to complete your account setup.</p>"
                    + "</body></html>";

            exchange.sendResponseHeaders(200, htmlResponse.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(htmlResponse.getBytes());
            os.close();

            if (code != null && onCodeReceived != null) {
                final String finalCode = code;
                Consumer<String> cb = onCodeReceived;
                onCodeReceived = null;
                cb.accept(finalCode);
            }
        }
    }

    public static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", url});
                } else if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                } else {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
