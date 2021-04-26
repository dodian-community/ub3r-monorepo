package net.dodian.uber.game.model.item;
// Scape - The Scape Developers Team

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.CreateGroundItem;

public class ItemHandler {
    // Phate: Setting VARS
    public static int showItemTimer = 60 * 2;
    public static int hideItemTimer = 60 * 4;

    // Phate: Global Item VARS
    public static int[] globalItemController = new int[8001];
    public static int[] globalItemID = new int[8001];
    public static int[] globalItemX = new int[8001];
    public static int[] globalItemY = new int[8001];
    public static int[] globalItemAmount = new int[8001];
    public static boolean[] globalItemStatic = new boolean[8001];

    public static int[] globalItemTicks = new int[8001];

    public ItemHandler() {
        for (int i = 0; i <= 8000; i++) {
            globalItemController[i] = 0;
            globalItemID[i] = 0;
            globalItemX[i] = 0;
            globalItemY[i] = 0;
            globalItemAmount[i] = 0;
            globalItemTicks[i] = 0;
            globalItemStatic[i] = false;
        }
        for (int i = 0; i < MaxDropItems; i++) {
            ResetItem(i);
        }
    }

    public static void createItemAll(int itemID, int itemX, int itemY, int itemAmount, int itemController) {
        for (int i = 0; i < PlayerHandler.players.length; i++) {
            Player p = PlayerHandler.players[i];
            if (p != null) {
                Client person = (Client) p;
                if ((person.getPlayerName() != null || person.getPlayerName() != "null")
                        && !(person.getSlot() == itemController)) {
                    if (person.distanceToPoint(itemX, itemY) <= 60) {
                        person.send(new CreateGroundItem(new GameItem(itemID, itemAmount), new Position(itemX, itemY)));
                    }
                }
            }
        }
    }

    public static void removeItemAll(int itemID, int itemX, int itemY) {
        for (int i = 0; i < PlayerHandler.players.length; i++) {
            Player p = PlayerHandler.players[i];
            if (p != null) {
                Client person = (Client) p;
                if (person.getPlayerName() != null || person.getPlayerName() != "null") {
                    // misc.println("distance to remove "+person.distanceToPoint(itemX,
                    // itemY));
                    if (person.distanceToPoint(itemX, itemY) <= 60) {
                        person.removeGroundItem(itemX, itemY, itemID);
                    }
                }
            }
        }
    }

    public static int MaxDropItems = 100000;
    public static int[] DroppedItemsID = new int[MaxDropItems];
    public static int[] DroppedItemsX = new int[MaxDropItems];
    public static int[] DroppedItemsY = new int[MaxDropItems];
    public static int[] DroppedItemsN = new int[MaxDropItems];
    public static int[] DroppedItemsH = new int[MaxDropItems];
    public static int[] DroppedItemsDDelay = new int[MaxDropItems];
    public static int[] DroppedItemsSDelay = new int[MaxDropItems];
    public static int[] DroppedItemsDropper = new int[MaxDropItems];
    public static int[] DroppedItemsDeletecount = new int[MaxDropItems];
    public static boolean[] DroppedItemsAlwaysDrop = new boolean[MaxDropItems];

    public void ResetItem(int ArrayID) {
        DroppedItemsID[ArrayID] = -1;
        DroppedItemsX[ArrayID] = -1;
        DroppedItemsY[ArrayID] = -1;
        DroppedItemsN[ArrayID] = -1;
        DroppedItemsH[ArrayID] = -1;
        DroppedItemsDDelay[ArrayID] = -1;
        DroppedItemsSDelay[ArrayID] = 0;
        DroppedItemsDropper[ArrayID] = -1;
        DroppedItemsDeletecount[ArrayID] = 0;
        DroppedItemsAlwaysDrop[ArrayID] = false;
    }
}
