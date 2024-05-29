package net.dodian.uber.game.model.item;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class ItemManager {
    public Map<Integer, Item> items = new HashMap<>();
    final int defaultStandAnim = 808, defaultWalkAnim = 819, defaultRunAnim = 824, defaultAttackAnim = 806;

    public ItemManager() {
        loadGlobalItems();
        loadItems();
        //Server.slots.loadGamble(); // Gamble :D
    }

    public void loadGlobalItems() {
        /* Troll items */
        Ground.addGroundItem(new Position(2611, 3096, 0), 11862, 1, 100);
        Ground.addGroundItem(new Position(2612, 3096, 0), 11863, 1, 100);
        Ground.addGroundItem(new Position(2563, 9511, 0), 1631, 1, 100);
        Ground.addGroundItem(new Position(2564, 9511, 0), 6571, 1, 100);
        /* Yanille starter items */
        Ground.addGroundItem(new Position(2605, 3104, 0), 1277, 1, 33);
        Ground.addGroundItem(new Position(2607, 3104, 0), 1171, 1, 33);
        /* Snape grass spawns!*/
        Ground.addGroundItem(new Position(2810, 3203, 0), 231, 1, 100);
        Ground.addGroundItem(new Position(2807, 3204, 0), 231, 1, 100);
        Ground.addGroundItem(new Position(2804, 3207, 0), 231, 1, 100);
        Ground.addGroundItem(new Position(2801, 3210, 0), 231, 1, 100);
        /* Limpwurt spawns!*/
        Ground.addGroundItem(new Position(2874, 3475, 0), 225, 1, 100);
        Ground.addGroundItem(new Position(2876, 3001, 0), 225, 1, 100);
        /* White berries spawns!*/
        Ground.addGroundItem(new Position(2935, 3489, 0), 239, 1, 100);
        Ground.addGroundItem(new Position(2877, 3000, 0), 239, 1, 100);
        /* Red Spider egg spawn*/
        Ground.addGroundItem(new Position(3595, 3479, 0), 223, 1, 100);
        Ground.addGroundItem(new Position(3597, 3479, 0), 223, 1, 100);
        /* Seaweed Brimhaven */
        Ground.addGroundItem(new Position(2797, 3211, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2795, 3212, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2792, 3213, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2789, 3214, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2786, 3215, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2784, 3217, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2782, 3219, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2779, 3219, 0), 401, 1, 25);
        /* Seaweed Bandit camp aka Sand crabs */
        Ground.addGroundItem(new Position(3143, 2991, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3143, 2988, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3145, 2985, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3147, 2983, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3149, 2980, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3147, 2977, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3145, 2975, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(3143, 2973, 0), 401, 1, 25);
        /* Seaweed Catherby */
        Ground.addGroundItem(new Position(2860, 3427, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2858, 3427, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2856, 3426, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2855, 3425, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2852, 3425, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2850, 3427, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2848, 3429, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2846, 3431, 0), 401, 1, 25);
        /* Seaweed Ardougne */
        Ground.addGroundItem(new Position(2641, 3255, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2644, 3254, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2645, 3252, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2645, 3250, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2643, 3248, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2641, 3246, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2641, 3243, 0), 401, 1, 25);
        Ground.addGroundItem(new Position(2642, 3240, 0), 401, 1, 25);

    }

    public void loadItems() {
        Statement s;
        try {
            try {
                s = getDbConnection().createStatement();
                ResultSet row = s.executeQuery("SELECT * FROM " + DbTables.GAME_ITEM_DEFINITIONS + " ORDER BY id ASC");
                while (row.next()) {
                    items.put(row.getInt("id"), new Item(row));
                }
                System.out.println("Loaded " + items.size() + " item definitions...");
                s.close();
            } catch (Exception e) {
                System.out.println("item load wrong 1.." + e);
            }
        } catch (Exception e) {
            System.out.println("item load wrong 2.." + e);
        }
    }

    public boolean isNote(int id) {
        Item i = items.get(id);
        return i != null && id >= 0 && i.getNoteable();
    }

    public boolean isStackable(int id) {
        Item i = items.get(id);
        return i != null && id >= 0 && i.getStackable();
    }

    public boolean isTwoHanded(int id) {
        Item i = items.get(id);
        return id >= 0 && i != null && i.getTwoHanded();
    }

    public int getSlot(int id) {
        if (id < 0)
            return 3;
        Item i = items.get(id);
        if (i == null)
            return 3;
        return i.getSlot();
    }

    public int getStandAnim(int id) {
        if (id < 1)
            return defaultStandAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultStandAnim;
        return i.getStandAnim();
    }

    public int getWalkAnim(int id) {
        if (id < 1)
            return defaultWalkAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultWalkAnim;
        return i.getWalkAnim();
    }

    public int getRunAnim(int id) {
        if (id < 1)
            return defaultRunAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultRunAnim;
        return i.getRunAnim();
    }

    public int getAttackAnim(int id) {
        if (id < 1)
            return defaultAttackAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultAttackAnim;
        return i.getAttackAnim();
    }

    public boolean isPremium(int id) {
        if (id < 0)
            return false;
        Item i = items.get(id);
        if (i == null)
            return false;
        return i.getPremium();
    }

    public boolean isTradable(int id) {
        if (id < 0)
            return false;
        if (id == 4084)
            return false;
        Item i = items.get(id);
        if (i == null)
            return false;
        return i.getTradeable();
    }

    public int getBonus(int id, int bonus) {
        if (id < 0)
            return 0;
        Item i = items.get(id);
        if (i == null)
            return 0;
        return i.getBonuses()[bonus];
    }

    public boolean isFullBody(int id) {
        if (id < 0)
            return false;
        Item i = items.get(id);
        return i != null && i.getSlot() == 4 && i.full;
    }

    public boolean isFullHelm(int id) {
        if (id < 0)
            return false;
        Item i = items.get(id);
        return i != null && i.getSlot() == 0 && i.full;
    }

    public boolean isMask(int id) {
        if (id < 0)
            return false;
        Item i = items.get(id);
        return i != null && i.getSlot() == 0 && i.mask;
    }

    public int getShopSellValue(int id) {
        Item i = items.get(id);
        if (i == null)
            return 1;
        return i.getShopSellValue();
    }

    public int getShopBuyValue(int id) {
        Item i = items.get(id);
        if (i == null)
            return 0;
        return i.getShopBuyValue();
    }

    public int getAlchemy(int id) {
        Item i = items.get(id);
        if (i == null)
            return 0;
        return i.getAlchemy();
    }

    public String getName(int id) {
        Item i = items.get(id);
        if (i == null)
            return "Database Error. Please contact admins with this error code: ITEM_NAME_" + id;
        return i.getName().replace("_", " ");
    }
    public String getExamine(int id) {
        Item i = items.get(id);
        if (i == null)
            return "";
        return i.getDescription().replace("_", " ");
    }

    public void getItemName(Client c, String name) {
        boolean send = false;
        name = name.replace("_", " ");
        for (Item i : items.values()) {
            if (name.equalsIgnoreCase(i.getName().replace("_", " ")) && !i.getDescription().equalsIgnoreCase("null")) {
                String prefix = "";
                if (isNote(i.getId()))
                    prefix = " (NOTED)";
                c.send(new SendMessage(Character.toUpperCase(name.charAt(0)) + name.substring(1) + prefix
                        + ": Sell Price: " + i.getShopBuyValue() + ". Alchemy Price: " + i.getAlchemy()));
                send = true;
            }
        }
        if (!send)
            c.send(new SendMessage("Could not find the item " + name + " in the database!"));
    }

    public void reloadItems() {
        items.clear();
        loadItems();
    }

}
