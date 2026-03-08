package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.runtime.interaction.ActiveInteraction;
import net.dodian.uber.game.runtime.interaction.InteractionIntent;
import net.dodian.uber.game.runtime.combat.CombatTargetState;
import net.dodian.uber.game.runtime.action.PlayerActionType;
import net.dodian.uber.game.runtime.scheduler.QueueTaskHandle;
import net.dodian.uber.game.runtime.tasking.GameTaskSet;
import net.dodian.uber.game.skills.mining.MiningState;
import net.dodian.uber.game.skills.woodcutting.WoodcuttingState;

final class PlayerInteractionState {
    private volatile InteractionIntent pendingInteraction;
    private volatile ActiveInteraction activeInteraction;
    private volatile long interactionEarliestCycle = 0L;
    private volatile QueueTaskHandle interactionTaskHandle;
    private volatile QueueTaskHandle farmDebugTaskHandle;
    private volatile QueueTaskHandle miningTaskHandle;
    private volatile QueueTaskHandle woodcuttingTaskHandle;
    private volatile QueueTaskHandle activeActionHandle;
    private volatile PlayerActionType activeActionType;
    private volatile long actionStartedCycle = 0L;
    private volatile CombatTargetState combatTargetState;
    private volatile GameTaskSet<?> playerTaskSet;
    private volatile MiningState miningState;
    private volatile WoodcuttingState woodcuttingState;

    InteractionIntent getPendingInteraction() {
        return pendingInteraction;
    }

    void setPendingInteraction(InteractionIntent pendingInteraction) {
        this.pendingInteraction = pendingInteraction;
    }

    ActiveInteraction getActiveInteraction() {
        return activeInteraction;
    }

    void setActiveInteraction(ActiveInteraction activeInteraction) {
        this.activeInteraction = activeInteraction;
    }

    long getInteractionEarliestCycle() {
        return interactionEarliestCycle;
    }

    void setInteractionEarliestCycle(long interactionEarliestCycle) {
        this.interactionEarliestCycle = interactionEarliestCycle;
    }

    QueueTaskHandle getInteractionTaskHandle() {
        return interactionTaskHandle;
    }

    void setInteractionTaskHandle(QueueTaskHandle interactionTaskHandle) {
        this.interactionTaskHandle = interactionTaskHandle;
    }

    void cancelInteractionTask() {
        QueueTaskHandle handle = interactionTaskHandle;
        interactionTaskHandle = null;
        if (handle != null) {
            handle.cancel();
        }
    }

    QueueTaskHandle getFarmDebugTaskHandle() {
        return farmDebugTaskHandle;
    }

    void setFarmDebugTaskHandle(QueueTaskHandle farmDebugTaskHandle) {
        this.farmDebugTaskHandle = farmDebugTaskHandle;
    }

    void cancelFarmDebugTask() {
        QueueTaskHandle handle = farmDebugTaskHandle;
        farmDebugTaskHandle = null;
        if (handle != null) {
            handle.cancel();
        }
    }

    QueueTaskHandle getMiningTaskHandle() {
        return miningTaskHandle;
    }

    void setMiningTaskHandle(QueueTaskHandle miningTaskHandle) {
        this.miningTaskHandle = miningTaskHandle;
    }

    void cancelMiningTask() {
        QueueTaskHandle handle = miningTaskHandle;
        miningTaskHandle = null;
        if (handle != null) {
            handle.cancel();
        }
    }

    MiningState getMiningState() {
        return miningState;
    }

    void setMiningState(MiningState miningState) {
        this.miningState = miningState;
    }

    void clearMiningState() {
        miningState = null;
    }

    QueueTaskHandle getWoodcuttingTaskHandle() {
        return woodcuttingTaskHandle;
    }

    void setWoodcuttingTaskHandle(QueueTaskHandle woodcuttingTaskHandle) {
        this.woodcuttingTaskHandle = woodcuttingTaskHandle;
    }

    void cancelWoodcuttingTask() {
        QueueTaskHandle handle = woodcuttingTaskHandle;
        woodcuttingTaskHandle = null;
        if (handle != null) {
            handle.cancel();
        }
    }

    WoodcuttingState getWoodcuttingState() {
        return woodcuttingState;
    }

    void setWoodcuttingState(WoodcuttingState woodcuttingState) {
        this.woodcuttingState = woodcuttingState;
    }

    void clearWoodcuttingState() {
        woodcuttingState = null;
    }

    QueueTaskHandle getActiveActionHandle() {
        return activeActionHandle;
    }

    void setActiveActionHandle(QueueTaskHandle activeActionHandle) {
        this.activeActionHandle = activeActionHandle;
    }

    PlayerActionType getActiveActionType() {
        return activeActionType;
    }

    void setActiveActionType(PlayerActionType activeActionType) {
        this.activeActionType = activeActionType;
    }

    long getActionStartedCycle() {
        return actionStartedCycle;
    }

    void setActionStartedCycle(long actionStartedCycle) {
        this.actionStartedCycle = actionStartedCycle;
    }

    void cancelActiveAction() {
        QueueTaskHandle handle = activeActionHandle;
        activeActionHandle = null;
        activeActionType = null;
        actionStartedCycle = 0L;
        if (handle != null) {
            handle.cancel();
        }
    }

    CombatTargetState getCombatTargetState() {
        return combatTargetState;
    }

    void setCombatTargetState(CombatTargetState combatTargetState) {
        this.combatTargetState = combatTargetState;
    }

    void clearCombatTargetState() {
        combatTargetState = null;
    }

    GameTaskSet<?> getPlayerTaskSet() {
        return playerTaskSet;
    }

    void setPlayerTaskSet(GameTaskSet<?> playerTaskSet) {
        this.playerTaskSet = playerTaskSet;
    }

    void terminatePlayerTasks() {
        cancelActiveAction();
        if (playerTaskSet != null) {
            playerTaskSet.terminateTasks();
            playerTaskSet = null;
        }
    }
}
