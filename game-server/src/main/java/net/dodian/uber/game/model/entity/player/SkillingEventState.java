package net.dodian.uber.game.model.entity.player;

public final class SkillingEventState {
    private final boolean randomEventOpen;
    private final int randomSkillId;
    private final int chestEventCount;
    private final boolean chestEventPendingMove;

    public SkillingEventState(
        boolean randomEventOpen,
        int randomSkillId,
        int chestEventCount,
        boolean chestEventPendingMove
    ) {
        this.randomEventOpen = randomEventOpen;
        this.randomSkillId = randomSkillId;
        this.chestEventCount = chestEventCount;
        this.chestEventPendingMove = chestEventPendingMove;
    }

    public boolean isRandomEventOpen() {
        return randomEventOpen;
    }

    public int getRandomSkillId() {
        return randomSkillId;
    }

    public int getChestEventCount() {
        return chestEventCount;
    }

    public boolean isChestEventPendingMove() {
        return chestEventPendingMove;
    }

    public SkillingEventState withRandomEventOpen(boolean value) {
        return new SkillingEventState(value, randomSkillId, chestEventCount, chestEventPendingMove);
    }

    public SkillingEventState withRandomSkillId(int value) {
        return new SkillingEventState(randomEventOpen, value, chestEventCount, chestEventPendingMove);
    }

    public SkillingEventState withChestEventCount(int value) {
        return new SkillingEventState(randomEventOpen, randomSkillId, value, chestEventPendingMove);
    }

    public SkillingEventState withChestEventPendingMove(boolean value) {
        return new SkillingEventState(randomEventOpen, randomSkillId, chestEventCount, value);
    }
}
