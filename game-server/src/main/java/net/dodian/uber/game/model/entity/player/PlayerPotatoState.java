package net.dodian.uber.game.model.entity.player;

public final class PlayerPotatoState {
    private final int flowType;
    private final int targetSlot;
    private final int targetIdentifier;
    private final int stage;

    public PlayerPotatoState(int flowType, int targetSlot, int targetIdentifier, int stage) {
        this.flowType = flowType;
        this.targetSlot = targetSlot;
        this.targetIdentifier = targetIdentifier;
        this.stage = stage;
    }

    public int getFlowType() {
        return flowType;
    }

    public int getTargetSlot() {
        return targetSlot;
    }

    public int getTargetIdentifier() {
        return targetIdentifier;
    }

    public int getStage() {
        return stage;
    }

    public boolean isActive() {
        return stage == 1;
    }
}
