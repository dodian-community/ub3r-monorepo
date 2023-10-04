package net.dodian.uber.comm;

import net.dodian.uber.game.model.entity.player.PlayerHandler;

public class Memory {

    private static Memory singleton = null;
    public long avgClockTime;

    public static Memory getSingleton() {
        if (singleton != null) {
            return singleton;
        } else {
            singleton = new Memory();
        }
        return singleton;
    }

    public int mb = 1024 * 1024;

    public void process() {
        Runtime r = Runtime.getRuntime();
        int onlinePlayer = PlayerHandler.getPlayerCount();
        int memoryUsage = (int) ((r.totalMemory() - r.freeMemory()) / mb);
        System.out.println("Players: " + onlinePlayer + " | Memory: " + memoryUsage + "MB | Cycle: " + avgClockTime);
    }

}
