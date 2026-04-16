package net.dodian.uber.game.model.entity.player;

public final class AgilitySessionState {
    private final int courseStage;
    private final long updatedAtCycle;

    public AgilitySessionState(int courseStage, long updatedAtCycle) {
        this.courseStage = courseStage;
        this.updatedAtCycle = updatedAtCycle;
    }

    public int getCourseStage() {
        return courseStage;
    }

    public long getUpdatedAtCycle() {
        return updatedAtCycle;
    }

    public AgilitySessionState withCourseStage(int stage, long cycle) {
        return new AgilitySessionState(stage, cycle);
    }
}
