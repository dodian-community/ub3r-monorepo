package net.dodian.uber.game.model.entity.player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;

import net.dodian.utilities.Cryption;

import net.dodian.utilities.Utils;

import static net.dodian.utilities.DotEnvKt.*;

public class ClientLoginHandler {

    private final Client client;
    private final SocketChannel socketChannel;

    public ClientLoginHandler(Client client, SocketChannel socketChannel) {
        this.client = client;
        this.socketChannel = socketChannel;
    }

    public void handleLogin() {
        client.isActive = false;//flag for if login is complete
        long serverSessionKey, clientSessionKey;
        serverSessionKey = ((long) (Math.random() * 99999999D) << 32)
                + (long) (Math.random() * 99999999D);
        System.out.println("Starting login process for client on slot: " + client.getSlot());
        try {
            client.returnCode = 2;
            ByteBuffer buffer = ByteBuffer.allocate(2);
            readFully(buffer);
            buffer.flip();
            int checkId = buffer.get() & 0xff;
            if (checkId != 14) {
                client.println_debug("Could not process client with id: " + checkId);
                client.disconnected = true;
                return;
            }
            buffer.get(); // Skip one byte

            ByteBuffer outBuffer = ByteBuffer.allocate(9);
            for (int i = 0; i < 8; i++) {
                outBuffer.put((byte) 10);
            }
            outBuffer.put((byte) 0);
            outBuffer.flip();
            socketChannel.write(outBuffer);

            ByteBuffer longBuffer = ByteBuffer.allocate(8);
            longBuffer.putLong(serverSessionKey);
            longBuffer.flip();
            socketChannel.write(longBuffer);
            //System.out.println("Sent server session key: " + serverSessionKey); -> Old Debug
            buffer.clear();
            readFully(buffer);
            buffer.flip();
            int loginType = buffer.get() & 0xff;
            //System.out.println("Received login type: " + loginType); -> Old Debug
            if (loginType != 16 && loginType != 18) {
                client.println_debug("Unexpected login type " + loginType);
                client.disconnected = true;
                return;
            }
            int loginPacketSize = buffer.get() & 0xff;
            int loginEncryptPacketSize = loginPacketSize - (36 + 1 + 1 + 2);
            //System.out.println("Login packet size: " + loginPacketSize + ", Encrypted size: " + loginEncryptPacketSize); -> Old Debug
            if (loginEncryptPacketSize <= 0) {
                client.println_debug("Zero RSA packet size!");
                client.disconnected = true;
                return;
            }

            ByteBuffer loginBuffer = ByteBuffer.allocate(loginPacketSize);
            readFully(loginBuffer);
            loginBuffer.flip();

            if (loginBuffer.get() != (byte)255 || loginBuffer.getShort() != 317) {
                client.returnCode = 6;
                client.disconnected = true;
                System.out.println("Invalid login protocol");
            }
            loginBuffer.get(); // Skip one byte
            for (int i = 0; i < 9; i++) {
                loginBuffer.getInt(); // Skip 9 ints
            }

            loginEncryptPacketSize--;
            int tmp = loginBuffer.get() & 0xff;
            if (loginEncryptPacketSize != tmp) {
                shutdownError("Encrypted packet data length (" + loginEncryptPacketSize
                        + ") different from length byte thereof (" + tmp + ")");
                return;
            }
            tmp = loginBuffer.get() & 0xff;
            if (tmp != 10) {
                shutdownError("Encrypted packet Id was " + tmp + " but expected 10");
                client.disconnected = true;
                return;
            }

            clientSessionKey = loginBuffer.getLong();
            serverSessionKey = loginBuffer.getLong();
            //System.out.println("Client session key: " + clientSessionKey + ", Server session key: " + serverSessionKey); -> Old Debug
            String customClientVersion = readString(loginBuffer);
            client.officialClient = customClientVersion.equals(getGameClientCustomVersion());
            if(!client.officialClient)
                client.returnCode = 6;
            client.disconnected = true;
            //client.UUID = readString(loginBuffer).split("-"); //TODO: Fix Better check on computer
            client.setPlayerName(readString(loginBuffer));
            if (client.getPlayerName() == null || client.getPlayerName().isEmpty()) {
                client.setPlayerName("player" + client.getSlot());
            }
            client.playerPass = readString(loginBuffer);
            String playerServer;
            try {
                playerServer = readString(loginBuffer);
            } catch (Exception e) {
                playerServer = "play.dodian.com";
            }
            //System.out.println("Player name: " + client.getPlayerName() + ", Server: " + playerServer); -> Old Debug!
            client.setPlayerName(client.getPlayerName().toLowerCase());
            char[] validChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
                    's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                    '_', ' '};
            client.setPlayerName(client.getPlayerName().trim());

            int[] sessionKey = new int[4];
            sessionKey[0] = (int) (clientSessionKey >> 32);
            sessionKey[1] = (int) clientSessionKey;
            sessionKey[2] = (int) (serverSessionKey >> 32);
            sessionKey[3] = (int) serverSessionKey;
            client.inStreamDecryption = new Cryption(sessionKey);
            for (int i = 0; i < 4; i++) {
                sessionKey[i] += 50;
            }
            client.outStreamDecryption = new Cryption(sessionKey);
            client.getOutputStream().packetEncryption = client.outStreamDecryption;

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
                    client.returnCode = 4;
                    client.disconnected = true;
                }
            }
            if (letters < 1) {
                client.returnCode = 3;
                client.disconnected = true;
            }

            char first = client.getPlayerName().charAt(0);
            client.properName = Character.toUpperCase(first) + client.getPlayerName().substring(1).toLowerCase();
            client.setPlayerName(client.properName.replace("_", " "));
            client.longName = Utils.playerNameToInt64(client.getPlayerName());
            if (Server.updateRunning && (Server.updateStartTime + (Server.updateSeconds * 1000L)) - System.currentTimeMillis() < 60000) {
                client.returnCode = 14;
                client.disconnected = true;
            }
            //System.out.println("Name check..." + client.longName + ":" + client.properName + " "); -> Old debug!
            int loadgame = Server.loginManager.loadgame(client, client.getPlayerName(), client.playerPass);
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
                    if(client.playerGroup == 2)
                        Server.loginManager.updatePlayerForumRegistration(client);
                    client.premium = true;
                    client.playerRights = 0;
            }
            for (String otherGroup : client.otherGroups) {
                if (otherGroup == null) {
                    continue;
                }
                String temp = otherGroup.trim();
                if (!temp.isEmpty()) {
                    int group = Integer.parseInt(temp);
                    switch (group) {
                        case 14: //Premium members but everyone is premium currently! Also can be handled in a better way!
                            break;
                        case 3:
                        case 19:
                            client.playerRights = 1;
                            break;
                    }
                }
            }
            if (loadgame == 0 && client.returnCode != 6) {
                client.validLogin = true;
                if (client.getPosition().getX() > 0 && client.getPosition().getY() > 0) {
                    client.transport(new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()));
                }
            } else {
                if (client.returnCode != 6 && client.returnCode != 5)
                    client.returnCode = loadgame;
                client.disconnected = true;
                client.teleportToX = 0;
                client.teleportToY = 0;
            }

            ByteBuffer responseBuffer = ByteBuffer.allocate(3);
            if (client.getSlot() == -1) {
                responseBuffer.put((byte) 7);
            } else if (playerServer.equals("INVALID")) {
                responseBuffer.put((byte) 10);
            } else {
                responseBuffer.put((byte) client.returnCode);
                if (client.returnCode == 21)
                    responseBuffer.put((byte) client.loginDelay);
            }
            responseBuffer.put((byte) (getGameWorldId() > 1 && client.playerRights < 2 ? 2 : client.playerRights));
            responseBuffer.put((byte) 0);
            responseBuffer.flip();
            socketChannel.write(responseBuffer);
            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
            if(client.validLogin && !client.disconnected) client.print_debug("....Success login of " + client.getPlayerName());
            //else client.print_debug("....Invalid login of " + client.getPlayerName() + ", " + client.returnCode + ", Rights: " + client.playerRights); -> Debug if needed!
        } catch (Exception __ex) {
            destruct();
            client.print_debug("....Failed destruct..." + __ex.getMessage());
            client.disconnected = true;
            //__ex.printStackTrace(); //Do we need?
            return;
        }

        if (client.getSlot() == -1 || client.returnCode != 2) {
            //client.print_debug("...slot="+ (client.getSlot() == -1 ? "-1" : "" + client.getSlot()) +" where return code is: " + client.returnCode); -> Old debug!
            return;
        }

        client.isActive = true;
        Thread mySocketThread = Server.createNewConnection(client.mySocketHandler);
        mySocketThread.start();
        client.packetSize = 0;
        client.packetType = -1;
        client.readPtr = 0;
        client.writePtr = 0;

        //System.out.println("Login process completed successfully for " + client.getPlayerName()); -> Old Debug!
    }


    private void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (socketChannel.read(buffer) == -1) {
                client.disconnected = true;
                throw new IOException("End of stream");
            }
        }
    }

    private String readString(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        byte b;
        while ((b = buffer.get()) != 10) {
            sb.append((char) b);
        }
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
            System.out.println("error in destruct " + ioe);
        }
        client.destruct();
    }


}