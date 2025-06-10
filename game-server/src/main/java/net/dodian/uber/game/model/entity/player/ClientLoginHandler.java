package net.dodian.uber.game.model.entity.player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.ServerConnectionHandler;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.utilities.Cryption;

import net.dodian.utilities.Utils;

import static net.dodian.utilities.DotEnvKt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the initial login protocol for a new client connection.
 * This includes handshake, credential validation, ISAAC cipher setup,
 * and transitioning the client to an active game state.
 */
public class ClientLoginHandler {

    private static final int EXPECTED_CHECK_ID = 14; // Initial byte sent by client to initiate login
    private static final int LOGIN_TYPE_NEW_CONNECTION = 16; // Indicates a fresh login attempt
    private static final int LOGIN_TYPE_RECONNECTION = 18;   // Indicates a reconnection attempt
    private static final int RSA_BLOCK_MAGIC_NUMBER = 255; // First byte of the RSA encrypted block
    private static final int CLIENT_VERSION_317 = 317;     // Expected client version
    private static final int RSA_PACKET_ID = 10;           // Identifier for the RSA block content
    private static final int LOGIN_SUCCESS_CODE = 2;       // Standard success code sent to client
    private static final int RETURN_CODE_INVALID_CLIENT_VERSION = 6; // Client version mismatch
    private static final int RETURN_CODE_BANNED_OR_NO_MEMBERSHIP = 5; // Account banned or lacks membership (original meaning)
    private static final int RETURN_CODE_SERVER_UPDATE_SOON = 14; // Server is updating soon
    // Other common return codes we don't use or use in here or I just hard coded it lol but might be useful:
//    private static final int RETURN_CODE_INVALID_CREDENTIALS = 3; // Invalid username or password
//    private static final int RETURN_CODE_ACCOUNT_DISABLED = 4;    // Account has been disabled (distinct from banned)
//    private static final int RETURN_CODE_ACCOUNT_ONLINE = 5;      // Account already logged in (overlaps with banned/membership)
//    private static final int RETURN_CODE_SERVER_FULL = 7;         // Server cannot accept more players
//    private static final int RETURN_CODE_LOGIN_SERVER_OFFLINE = 8; // Login server offline
//    private static final int RETURN_CODE_LOGIN_LIMIT_EXCEEDED = 9; // Too many connections from your address
//    private static final int RETURN_CODE_BAD_SESSION_ID = 10;     // Bad session ID (reconnection specific)
//    private static final int RETURN_CODE_PLEASE_TRY_AGAIN = 11;   // Generic "try again"
//    private static final int RETURN_CODE_WORLD_FULL_MEMBERS = 12; // Members world full
//    private static final int RETURN_CODE_COULD_NOT_COMPLETE = 13; // Could not complete login
//    private static final int RETURN_CODE_UPDATE_IN_PROGRESS = 14; // Server updating (same as SERVER_UPDATE_SOON)
//    private static final int RETURN_CODE_LOGIN_ATTEMPTS_EXCEEDED = 16; // Too many login attempts
//    private static final int RETURN_CODE_MEMBERS_AREA = 17;       // Attempt to login to members area on free world
//    private static final int RETURN_CODE_INVALID_LOGIN_SERVER = 20; // Invalid login server requested
//    private static final int RETURN_CODE_PROFILE_TRANSFER = 21;   // Account profile transfer in progress


    private final Client client;
    private final SocketChannel socketChannel;

    private static final Logger logger = LoggerFactory.getLogger(ClientLoginHandler.class);


    private static class LoginPacketDetails {
        final int loginType;
        final int loginPacketSize; // This is the byte value read from stream
        final int loginEncryptPacketSize; // This is calculated (loginPacketSize - RSA overhead)

        LoginPacketDetails(int loginType, int loginPacketSize, int loginEncryptPacketSize) {
            this.loginType = loginType;
            this.loginPacketSize = loginPacketSize;
            this.loginEncryptPacketSize = loginEncryptPacketSize;
        }
    }

    public ClientLoginHandler(Client client, SocketChannel socketChannel) {
        this.client = client;
        this.socketChannel = socketChannel;
    }

    /**
     * Performs the initial part of the login handshake.
     * Reads the connection type ID (EXPECTED_CHECK_ID) and sends back initial server data
     * including parts of the server session key.
     * @param serverSessionKey The server-generated session key to send to the client.
     * @return true if the handshake step is successful, false otherwise (and sets client.disconnected).
     * @throws IOException If an I/O error occurs.
     */
    private boolean performInitialHandshake(long serverSessionKey) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        readFully(buffer);
        buffer.flip();
        int checkId = buffer.get() & 0xff;
        if (checkId != EXPECTED_CHECK_ID) {
            client.println_debug("Could not process client with id: " + checkId);
            client.disconnected = true;
            return false;
        }


        ByteBuffer outBuffer = ByteBuffer.allocate(9);
        for (int i = 0; i < 8; i++) {
            outBuffer.put((byte) 10);
        }
        outBuffer.put((byte) 0); // Final byte is 0
        outBuffer.flip();
        socketChannel.write(outBuffer);

        ByteBuffer longBuffer = ByteBuffer.allocate(8);
        longBuffer.putLong(serverSessionKey);
        longBuffer.flip();
        socketChannel.write(longBuffer);
        return true;
    }



    /**
     * Reads and validates the login packet header.
     * This includes the login type (new connection or reconnection) and the size of the login packet.
     * @return A LoginPacketDetails object if the header is valid, or null if validation fails (and sets client.disconnected).
     * @throws IOException If an I/O error occurs.
     */
    private LoginPacketDetails readLoginPacketHeader() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2); // Buffer for loginType and loginPacketSizeByte
        readFully(buffer);
        buffer.flip();
        int loginType = buffer.get() & 0xff;
        if (loginType != LOGIN_TYPE_NEW_CONNECTION && loginType != LOGIN_TYPE_RECONNECTION) {
            client.println_debug("Unexpected login type " + loginType);
            client.disconnected = true;
            return null;
        }
        int loginPacketSizeByte = buffer.get() & 0xff;
        int loginEncryptPacketSize = loginPacketSizeByte - (36 + 1 + 1 + 2); // RSA overhead
        if (loginEncryptPacketSize <= 0) {
            client.println_debug("Zero RSA packet size!");
            client.disconnected = true;
            return null;
        }
        return new LoginPacketDetails(loginType, loginPacketSizeByte, loginEncryptPacketSize);
    }

    /**
     * Processes the main encrypted login payload from the client.
     * Validates protocol specifics (magic number, client version, RSA packet ID),
     * extracts session keys, username, password, and custom client version.
     * Sets up ISAAC ciphers on the client object for stream encryption/decryption.
     * @param details The details from the login packet header.
     * @param loginBuffer The ByteBuffer containing the login payload, already filled and flipped.
     * @param outSessionKeys A long array of size 2 to store the [clientSessionKey, serverSessionKeyFromPayload].
     * @return true if payload processing is successful, false otherwise (and sets client.returnCode/client.disconnected).
     * @throws IOException If an I/O error occurs.
     */
    private boolean processLoginPayload(LoginPacketDetails details, ByteBuffer loginBuffer, long[] outSessionKeys) throws IOException {
        if (loginBuffer.get() != (byte) RSA_BLOCK_MAGIC_NUMBER || loginBuffer.getShort() != CLIENT_VERSION_317) {
            client.returnCode = RETURN_CODE_INVALID_CLIENT_VERSION;
            client.disconnected = true;
            System.out.println("Invalid login protocol: Magic number or client version mismatch.");
            return false;
        }
        loginBuffer.get(); // Skip low memory version byte
        for (int i = 0; i < 9; i++) {
            loginBuffer.getInt(); // Skip 9 ints (CRC keys)
        }

        int encryptedPacketLengthByte = loginBuffer.get() & 0xff;
        if ((details.loginEncryptPacketSize - 1) != encryptedPacketLengthByte) {
            shutdownError("Encrypted packet data length (" + (details.loginEncryptPacketSize - 1)
                    + ") different from length byte thereof (" + encryptedPacketLengthByte + ")");
            return false; // shutdownError calls destruct which sets disconnected
        }

        int rsaPacketId = loginBuffer.get() & 0xff;
        if (rsaPacketId != RSA_PACKET_ID) {
            shutdownError("Encrypted packet Id was " + rsaPacketId + " but expected " + RSA_PACKET_ID);
            // shutdownError should handle this if it calls destruct.
            return false;
        }

        outSessionKeys[0] = loginBuffer.getLong(); // clientSessionKey
        outSessionKeys[1] = loginBuffer.getLong(); // serverSessionKeyFromPayload

        String customClientVersion = readString(loginBuffer);
        client.officialClient = customClientVersion.equals(getGameClientCustomVersion());
        if (!client.officialClient) {
            client.returnCode = RETURN_CODE_INVALID_CLIENT_VERSION;
            client.disconnected = true;
            System.out.println("Custom client version mismatch. Expected: " + getGameClientCustomVersion() + " Got: " + customClientVersion);
            return false;
        }

        // client.UUID = readString(loginBuffer).split("-"); //TODO: Fix Better check on computer
        client.setPlayerName(readString(loginBuffer));
        if (client.getPlayerName() == null || client.getPlayerName().isEmpty()) {
            client.setPlayerName("player" + client.getSlot()); // Default name if empty
        }
        client.playerPass = readString(loginBuffer);
        String playerServer;
        if (loginBuffer.hasRemaining()) {
            playerServer = readString(loginBuffer);
        } else {
            playerServer = "";
        }
        // System.out.println("Player connected via server address: " + playerServer);

        // ISAAC Cipher Setup
        int[] sessionKeySetup = new int[4];
        sessionKeySetup[0] = (int) (outSessionKeys[0] >> 32); // clientSessionKey
        sessionKeySetup[1] = (int) outSessionKeys[0];
        sessionKeySetup[2] = (int) (outSessionKeys[1] >> 32); // serverSessionKeyFromPayload
        sessionKeySetup[3] = (int) outSessionKeys[1];
        client.inStreamDecryption = new Cryption(sessionKeySetup);
        for (int i = 0; i < 4; i++) {
            sessionKeySetup[i] += 50;
        }
        client.outStreamDecryption = new Cryption(sessionKeySetup);
        client.getOutputStream().packetEncryption = client.outStreamDecryption;
        client.getOutputStream().packetEncryption = client.outStreamDecryption;
        return true;
    }

    /**
     * Validates client details (like username constraints) and attempts to load the player's account.
     * This includes checking name validity, server update status, loading data via LoginManager,
     * and setting player rights and premium status based on loaded group information.
     * @return true if validation passes and account loading is successful (or a specific recoverable error code is set),
     *         false for critical failures that should terminate login (and sets client.disconnected/client.returnCode).
     * @throws IOException If an I/O error occurs (though less likely here, more from network ops).
     */
    private boolean validateClientDetailsAndLoadAccount() throws IOException {
        // Player name validation (name is expected to be lowercased before this method)
        char[] validChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
                's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                '_', ' '};
        client.setPlayerName(client.getPlayerName().trim()); // Trim before validation

        int letters = 0;
        for (int i = 0; i < client.getPlayerName().length(); i++) {
            boolean valid = false;
            for (char validChar : validChars) {
                if (client.getPlayerName().charAt(i) == validChar) {
                    valid = true;
                    break;
                }
            }
            if (valid && client.getPlayerName().charAt(i) != '_' && client.getPlayerName().charAt(i) != ' ') {
                letters++;
            }
            if (!valid) {
                client.returnCode = 4; // RETURN_CODE_INVALID_NAME_CHARS
                client.disconnected = true;
                return false;
            }
        }
        if (letters < 1) { // Min 1 letter (not allowing names like "___" or "   ")
            client.returnCode = 3; // RETURN_CODE_NAME_TOO_SHORT
            client.disconnected = true;
            return false;
        }

        // Capitalize and format name
        char first = client.getPlayerName().charAt(0);
        client.properName = Character.toUpperCase(first) + client.getPlayerName().substring(1).toLowerCase(); // Already toLowerCase, but good practice
        client.setPlayerName(client.properName.replace("_", " "));
        client.longName = Utils.playerNameToInt64(client.getPlayerName());

        // Check server update status
        if (Server.updateRunning && (Server.updateStartTime + (Server.updateSeconds * 1000L)) - System.currentTimeMillis() < 60000) {
            client.returnCode = RETURN_CODE_SERVER_UPDATE_SOON;
            client.disconnected = true;
            return false;
        }

        // Load game
        int loadGameResult = Server.loginManager.loadgame(client, client.getPlayerName(), client.playerPass);

        // Process playerGroup and otherGroups for rights and premium status
        switch (client.playerGroup) {
            case 6: // root admin
            case 18: // root admin
            case 10: // content dev
                client.playerRights = 2;
                client.premium = true;
                break;
            case 9: // player moderator
            case 5: // global mod
                client.playerRights = 1;
                client.premium = true;
                break;
            default:
                if (client.playerGroup == 2) {
                    Server.loginManager.updatePlayerForumRegistration(client);
                }
                client.premium = true; // Default premium status, adjust if needed
                client.playerRights = 0;
        }
        for (String otherGroupStr : client.otherGroups) {
            if (otherGroupStr == null) continue;
            String temp = otherGroupStr.trim();
            if (!temp.isEmpty()) {
                int group = Integer.parseInt(temp);
                // Example: group 3 or 19 might grant rights=1, group 14 might be premium (already true)
                if (group == 3 || group == 19) {
                    client.playerRights = Math.max(client.playerRights, 1); // Ensure it doesn't override higher rights
                }
                // Add other group mappings if necessary
            }
        }
        // Ensure premium doesn't downgrade rights if rights imply premium
        if(client.playerRights > 0) client.premium = true;


        if (loadGameResult == 0 && client.returnCode != RETURN_CODE_INVALID_CLIENT_VERSION) {
            client.validLogin = true;
            if (client.getPosition().getX() > 0 && client.getPosition().getY() > 0) {

                client.transport(new Position(client.getPosition().getX(),
                        client.getPosition().getY(), client.getPosition().getZ()));

            }
        } else { // loadgame failed or other issue
            if (client.returnCode != RETURN_CODE_INVALID_CLIENT_VERSION && client.returnCode != RETURN_CODE_BANNED_OR_NO_MEMBERSHIP) {
                client.returnCode = loadGameResult; // Set return code from loadgame result
            }
            client.disconnected = true;
            client.teleportToX = 0; // Reset position for safety if login fails
            client.teleportToY = 0;
            return false;
        }
        return true;
    }

    /**
     * Sends the final login response code to the client.
     * The response includes the status code, player rights, and a flag related to world ID.
     * @throws IOException If an I/O error occurs during writing to the socket.
     */
    private void sendLoginResponseToClient() throws IOException {
        ByteBuffer responseBuffer = ByteBuffer.allocate(3);
        if (client.getSlot() == -1) {
            responseBuffer.put((byte) 7); // RETURN_CODE_SERVER_FULL
        } else {
            responseBuffer.put((byte) client.returnCode);
            if (client.returnCode == 21) { // RETURN_CODE_LOGIN_DELAY
                responseBuffer.put((byte) client.loginDelay);
            }
        }
        responseBuffer.put((byte) (getGameWorldId() > 1 && client.playerRights < 2 ? 2 : client.playerRights));
        responseBuffer.put((byte) 0); // End of packet marker
        responseBuffer.flip();
        socketChannel.write(responseBuffer);
    }

    /**
     * Transitions the client to an active game session after a successful login.
     * This involves setting the client as active, starting its network handler thread,
     * and resetting packet-related attributes for the game session.
     */
    private void transitionToActiveGameSession() {
        Thread mySocketThread = Server.createNewConnection(client.mySocketHandler);
        mySocketThread.start();
        client.isActive = true;
        client.packetSize = 0;
        client.packetType = -1;
        client.readPtr = 0;
        client.writePtr = 0;
        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);//i don't think its needed?

        //moved to stage 5 can delete this if delay in loading region is fixed
        //client.transport(new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()));


    }

    public void handleLogin() {
        client.isActive = false;
        long serverSessionKey = ((long) (Math.random() * 99999999.0) << 32) + (long) (Math.random() * 99999999.0);
        logger.info("Starting login process for client on slot: " + client.getSlot());
        // System.out.println("Starting login process for client on slot: " + client.getSlot());
        try {
            client.returnCode = LOGIN_SUCCESS_CODE; // Default to success

            // Stage 1: Initial Handshake
            if (!performInitialHandshake(serverSessionKey)) {
                // performInitialHandshake sets disconnected and logs internally on failure.
                return;
            }

            // Stage 2: Read Login Packet Header
            LoginPacketDetails details = readLoginPacketHeader();
            if (details == null) {
                // readLoginPacketHeader sets disconnected and logs internally on failure.
                return;
            }

            // Stage 3: Read Login Block & Process Login Payload
            ByteBuffer loginBlockBuffer = ByteBuffer.allocate(details.loginPacketSize);
            readFully(loginBlockBuffer); // Read the entire login block based on size from header
            loginBlockBuffer.flip();

            long[] sessionKeys = new long[2]; // For client and server session keys from payload
            if (!processLoginPayload(details, loginBlockBuffer, sessionKeys)) {
                // processLoginPayload sets returnCode and disconnected status on failure.
                // If it's an invalid client version, send that specific response.
                if (client.returnCode == RETURN_CODE_INVALID_CLIENT_VERSION && client.disconnected) {
                    sendLoginResponseToClient();
                }
                return;
            }

            // Ensure player name is consistently lowercased before validation and loading.
            client.setPlayerName(client.getPlayerName().toLowerCase());

            // Stage 4: Validate Client Details and Load Account
            if (!validateClientDetailsAndLoadAccount()) {
                // validateClientDetailsAndLoadAccount sets client.disconnected and client.returnCode.
                // The response will be sent by the sendLoginResponseToClient call below.
                // No specific action needed here other than falling through to send the response.
            }

            // Stage 5: Send Login Response to Client
            // This is sent regardless of success or specific failure codes set by previous stages.
            sendLoginResponseToClient();
            client.transport(new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()));

            // Stage 6: Check for final success and transition to active game session
            if (client.getSlot() == -1 || client.returnCode != LOGIN_SUCCESS_CODE) {
                // If slot is -1 (e.g. server full) or any other error code was set, disconnect.
                client.disconnected = true;
                return;
            }

            // If all checks passed and returnCode is still LOGIN_SUCCESS_CODE:
            transitionToActiveGameSession();
            if (client.validLogin && !client.disconnected) {
                logger.info("Successfully logged in player: {}", client.getPlayerName());
            }

        } catch (IOException e) {
            // Catch I/O errors from socket operations
            client.println_debug("IOException during login for " + (client.getPlayerName() != null ? client.getPlayerName() : "unknown") + ": " + e.getMessage());
            client.disconnected = true;
        } catch (Exception e) {
            String playerName = (client != null && client.getPlayerName() != null) ? client.getPlayerName() : "unknown";
            System.err.println("Unexpected exception during login for " + playerName + ":");
            e.printStackTrace();
            client.println_debug("Unexpected exception during login: " + e.getMessage());
            client.disconnected = true;
        } finally {
            if (client.disconnected && !client.isActive) {
                // isActive check ensures we don't destruct if already transitioned to game session
                destruct();
            }
        }
    }

    private void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (socketChannel.read(buffer) == -1) {
                client.disconnected = true;
                throw new IOException("End of stream for readFully");
            }
        }
    }

    private String readString(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == 10) {
                return sb.toString();
            }
            sb.append((char) b);
        }
        System.err.println("[Login] Buffer underflow or malformed string: missing newline terminator. Partial string: '" + sb + "'. Remaining: " + buffer.remaining());
        return sb.toString();
    }

    private void shutdownError(String errorMessage) {
        Utils.println(": " + errorMessage);
        destruct();
    }

    private void destruct() {
        if (socketChannel == null) {
            return;
        } // already shutdown
        try {
            if (client.saveNeeded)
                client.saveStats(true);
            socketChannel.close();
            client.disconnected = true;
        } catch (java.io.IOException ioe) {
            System.out.println("error in destruct for login " + ioe);
        }
        client.destruct();
    }
}
