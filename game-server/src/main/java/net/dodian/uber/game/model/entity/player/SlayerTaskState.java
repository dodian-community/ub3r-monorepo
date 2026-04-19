package net.dodian.uber.game.model.entity.player;

public final class SlayerTaskState {
    private final int masterNpcId;
    private final int taskOrdinal;
    private final int assignmentAmount;
    private final int remainingAmount;
    private final int streak;
    private final int points;
    private final int blockedTaskOrdinal;

    public SlayerTaskState(
        int masterNpcId,
        int taskOrdinal,
        int assignmentAmount,
        int remainingAmount,
        int streak,
        int points,
        int blockedTaskOrdinal
    ) {
        this.masterNpcId = masterNpcId;
        this.taskOrdinal = taskOrdinal;
        this.assignmentAmount = assignmentAmount;
        this.remainingAmount = remainingAmount;
        this.streak = streak;
        this.points = points;
        this.blockedTaskOrdinal = blockedTaskOrdinal;
    }

    public int getMasterNpcId() {
        return masterNpcId;
    }

    public int getTaskOrdinal() {
        return taskOrdinal;
    }

    public int getAssignmentAmount() {
        return assignmentAmount;
    }

    public int getRemainingAmount() {
        return remainingAmount;
    }

    public int getStreak() {
        return streak;
    }

    public int getPoints() {
        return points;
    }

    public int getBlockedTaskOrdinal() {
        return blockedTaskOrdinal;
    }

    public SlayerTaskState withRemainingAmount(int value) {
        return new SlayerTaskState(masterNpcId, taskOrdinal, assignmentAmount, value, streak, points, blockedTaskOrdinal);
    }

    public SlayerTaskState withAssignment(int masterId, int ordinal, int assignment, int remaining) {
        return new SlayerTaskState(masterId, ordinal, assignment, remaining, streak, points, blockedTaskOrdinal);
    }

    public SlayerTaskState withMasterNpcId(int value) {
        return new SlayerTaskState(value, taskOrdinal, assignmentAmount, remainingAmount, streak, points, blockedTaskOrdinal);
    }

    public SlayerTaskState withTaskOrdinal(int value) {
        return new SlayerTaskState(masterNpcId, value, assignmentAmount, remainingAmount, streak, points, blockedTaskOrdinal);
    }

    public SlayerTaskState withStreak(int value) {
        return new SlayerTaskState(masterNpcId, taskOrdinal, assignmentAmount, remainingAmount, value, points, blockedTaskOrdinal);
    }

    public SlayerTaskState withPoints(int value) {
        return new SlayerTaskState(masterNpcId, taskOrdinal, assignmentAmount, remainingAmount, streak, value, blockedTaskOrdinal);
    }
}
