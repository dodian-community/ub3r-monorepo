package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;

public class GameItem {

    private final int id;
    private int amount;
    private final boolean stackable;

    public GameItem(int id, int amount) {
        stackable = Server.itemManager.isStackable(id);
        this.id = id;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void addAmount(int amount) {
        this.amount += amount;
    }

    public void removeAmount(int amount) {
        this.amount -= amount;
    }

    public boolean isStackable() {
        return stackable;
    }

}