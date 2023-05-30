package net.dodian.uber.game.model.item;

public class Equipment extends ItemContainer {

    public static final int SIZE = 14;

    public Equipment() {
        super(SIZE);
    }

    public GameItem getSlot(Slot slot) {
        return getSlot(slot.getId());
    }

    public void setSlot(Slot slot, GameItem item) {
        setSlot(slot.getId(), item);
    }

    public enum Slot {

        HEAD(0), CAPE(1), NECK(2), WEAPON(3), CHEST(4), SHIELD(5), LEGS(7), BLESSING(8), HANDS(9), FEET(10), RING(12), ARROWS(13);

        private int slotId;

        Slot(int slotId) {
            this.slotId = slotId;
        }

        public int getId() {
            return slotId;
        }

    }

}
