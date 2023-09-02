package net.dodian.client;

import com.jagex.runescape.Client;
import com.jagex.runescape.sign.Signlink;
import com.jagex.runescape2.Game;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GameClient {

    public static void main(String[] args) {
        Game.setHighmem();
        Game.portOffset = 0;
        Game.nodeID = 0;
        Game.members = false;
        Game.crc32Check = false;
        Game.server = "127.0.0.1";

        com.jagex.runescape2.Signlink.storeid = 1;
        com.jagex.runescape2.Signlink.startpriv();

        Game game = new Game();
        game.jaggrabEnabled = false;
        game.init(765, 503);
    }

    public static void main1(String[] args) throws UnknownHostException {
        Client.localWorldId = 0;
        Client.portOffset = 0;
        Client.setHighMem();
        Client.membersWorld = false;

        Signlink.storeid = 1;
        Signlink.startpriv(InetAddress.getLocalHost());

        Client client = new Client();
        client.createClientFrame(765, 503);
    }
}