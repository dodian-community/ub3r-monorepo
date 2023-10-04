package net.dodian.uber.game.model.item;

import java.util.ArrayList;

public class ItemContainer {

    private ArrayList<GameItem> items;

    public ItemContainer(int size) {
        this.items = new ArrayList<GameItem>();
        for (int i = 0; i < size; i++)
            items.add(new GameItem(-1, 0));
    }

    protected GameItem getSlot(int slot) {
        return items.get(slot);
    }

    protected void setSlot(int slot, GameItem item) {
        items.set(slot, item);
    }

}
