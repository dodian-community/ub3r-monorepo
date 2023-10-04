package net.dodian.uber.game.party;

import net.dodian.uber.game.model.Position;

public class RewardItem {

    private int id = -1;
    private int amount = 0;
    private Position pos;

    public RewardItem(int id) {
        this.id = id;
        this.amount = 1;
        this.pos = null;
    }

    public RewardItem(int id, int amount) {
        this.id = id;
        this.amount = amount;
        this.pos = null;
    }

    public RewardItem(int id, int amount, Position pos) {
        this.id = id;
        this.amount = 1;
        this.pos = pos;
    }

    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount += amount;
    }

    public Position getPosition() {
        return pos;
    }

    public void setPosition(Position pos) {
        this.pos = pos;
    }

}
