package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class ItemManager {
    public Map<Integer, Item> items = new HashMap<Integer, Item>();
    final int defaultStandAnim = 808, defaultWalkAnim = 819, defaultRunAnim = 824, defaultAttackAnim = 806;

    public ItemManager() {
        loadItems();
        Server.slots.loadGamble(); // Gamble :D
    }

    public void loadItems() {
        Statement s = null;
        try {
            try {
                s = getDbConnection().createStatement();
                ResultSet row = s.executeQuery("SELECT * FROM " + DbTables.GAME_ITEM_DEFINITIONS + " ORDER BY id ASC");
                while (row.next()) {
                    items.put(row.getInt("id"), new Item(row));
                }
                System.out.println("Loaded " + items.size() + " item definitions...");
                row.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                s.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isNote(int id) {
        Item i = items.get(id);
        return i == null || id < 0 ? false : i.getNoteable();
    }

    public boolean isStackable(int id) {
        Item i = items.get(id);
        return i == null || id < 0 ? false : i.getStackable();
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
        if (id < 0)
            return defaultStandAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultStandAnim;
        return i.getStandAnim();
    }

    public int getWalkAnim(int id) {
        if (id < 0)
            return defaultWalkAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultWalkAnim;
        return i.getWalkAnim();
    }

    public int getRunAnim(int id) {
        if (id < 0)
            return defaultRunAnim;
        Item i = items.get(id);
        if (i == null)
            return defaultRunAnim;
        return i.getRunAnim();
    }

    public int getAttackAnim(int id) {
        if (id < 0)
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
        if (i != null && i.getSlot() == 4 && i.full) {
            return true;
        }
        return false;
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

    public void getItemName(Client c, String name) {
        boolean send = false;
        name = name.replace("_", " ");
        for (Item i : items.values()) {
            if (name.equalsIgnoreCase(i.getName().replace("_", " ")) && !i.getDescription().equalsIgnoreCase("null")) {
                String prefix = "";
                if (isNote(i.getId()))
                    prefix = " (NOTED)";
                c.send(new SendMessage(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "" + prefix
                        + ": Sell Price: " + i.getShopBuyValue() + ". Alchemy Price: " + i.getAlchemy() + ""));
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
