package net.dodian.uber.game.model;

/**
 * @author Dashboard
 */
public class WalkToTask {

    private Action action;
    private int walkToId;
    private Position walkToPosition;

    public WalkToTask(Action action, int walkToId, Position walkToPosition) {
        this.action = action;
        this.walkToId = walkToId;
        this.walkToPosition = walkToPosition;
    }

    public Action getWalkToAction() {
        return this.action;
    }

    public int getWalkToId() {
        return this.walkToId;
    }

    public Position getWalkToPosition() {
        return this.walkToPosition;
    }

    /**
     * Compares the walk to tasks. If they are accomplishing the same thing, they
     * are the same.
     */
    @Override
    public boolean equals(Object o) {
        WalkToTask other = (WalkToTask) o;
        return other.getWalkToAction() == this.getWalkToAction() && other.getWalkToId() == this.getWalkToId()
                && other.getWalkToPosition() == this.getWalkToPosition();
    }

    public enum Action {
        OBJECT_FIRST_CLICK, OBJECT_SECOND_CLICK, OBJECT_THIRD_CLICK, NPC_FIRST_CLICK, NPC_SECOND_CLICK, NPC_THIRD_CLICK, ITEM_ON_OBJECT, ITEM_ON_NPC, ATTACK_NPC, ATTACK_PLAYER;
    }

}
