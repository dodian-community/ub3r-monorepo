package net.dodian.uber.game.model.entity.player;

public final class MovementLockState {
    private final String source;
    private final long startedCycle;

    public MovementLockState(String source, long startedCycle) {
        this.source = source;
        this.startedCycle = startedCycle;
    }

    public String getSource() {
        return source;
    }

    public long getStartedCycle() {
        return startedCycle;
    }
}
