package net.dodian.client;

import com.jagex.runescape2.Game;
import com.jagex.runescape2.Signlink;

public class GameClient {

    public static void main(String[] args) {
        Game.setHighmem();
        Game.portOffset = 0;
        Game.nodeID = 0;
        Game.members = false;
        Game.crc32Check = false;
        Game.server = "127.0.0.1";

        Signlink.storeid = 1;
        Signlink.startpriv();

        Game game = new Game();
        game.jaggrabEnabled = false;
        game.init(765, 503);
    }
}