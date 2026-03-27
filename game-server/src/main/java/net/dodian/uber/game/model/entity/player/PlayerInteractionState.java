package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.runtime.interaction.ActiveInteraction;
import net.dodian.uber.game.runtime.interaction.InteractionIntent;
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason;
import net.dodian.uber.game.runtime.action.PendingProductionSelection;
import net.dodian.uber.game.runtime.combat.CombatCancellationReason;
import net.dodian.uber.game.runtime.combat.CombatTargetState;
import net.dodian.uber.game.runtime.lifecycle.DeathTaskState;
import net.dodian.uber.game.runtime.action.ActiveProductionSelection;
import net.dodian.uber.game.runtime.action.PlayerActionType;
import net.dodian.uber.game.runtime.scheduler.QueueTaskHandle;
import net.dodian.uber.game.runtime.tasking.GameTaskSet;
import net.dodian.uber.game.skills.smithing.ActiveSmithingSelection;
import net.dodian.uber.game.skills.smithing.SmeltingSelection;
import net.dodian.uber.game.skills.mining.MiningState;
import net.dodian.uber.game.skills.woodcutting.WoodcuttingState;
import net.dodian.uber.game.skills.fletching.FletchingState;
import net.dodian.uber.game.skills.fishing.FishingState;
import net.dodian.uber.game.skills.cooking.CookingState;
import net.dodian.uber.game.skills.crafting.CraftingState;
import net.dodian.uber.game.skills.prayer.PrayerOfferingState;
import net.dodian.uber.game.skills.runecrafting.RunecraftingState;
import net.dodian.uber.game.skills.thieving.plunder.PyramidPlunderPlayerState;
import net.dodian.uber.game.runtime.interaction.InteractionAnchorState;
import java.util.concurrent.ConcurrentHashMap;

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
    private volatile PlayerActionCancelReason activeActionCancelReason;
    private volatile PlayerActionCancelReason lastActionCancelReason;
    private volatile long lastActionCancelCycle = 0L;
    private volatile CombatTargetState combatTargetState;
    private volatile CombatCancellationReason combatCancellationReason;
    private volatile long combatLogoutLockUntilCycle = 0L;
    private volatile long lastBlockAnimationCycle = -1L;
    private volatile DeathTaskState deathTaskState;
    private volatile ActiveSmithingSelection activeSmithingSelection;
    private volatile SmeltingSelection smeltingSelection;
    private volatile int pendingSmeltingBarId = -1;
    private volatile PendingProductionSelection pendingProductionSelection;
    private volatile ActiveProductionSelection activeProductionSelection;
    private volatile GameTaskSet<?> playerTaskSet;
    private volatile MiningState miningState;
    private volatile WoodcuttingState woodcuttingState;
    private volatile FletchingState fletchingState;
    private volatile FishingState fishingState;
    private volatile CookingState cookingState;
    private volatile CraftingState craftingState;
    private volatile PrayerOfferingState prayerOfferingState;
    private volatile RunecraftingState runecraftingState;
    private volatile InteractionAnchorState interactionAnchorState;
    private volatile PyramidPlunderPlayerState pyramidPlunderState;
    private final ConcurrentHashMap<String, Long> throttleUntilCycles = new ConcurrentHashMap<>();

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

    FletchingState getFletchingState() {
        return fletchingState;
    }

    void setFletchingState(FletchingState fletchingState) {
        this.fletchingState = fletchingState;
    }

    void clearFletchingState() {
        fletchingState = null;
    }

    FishingState getFishingState() {
        return fishingState;
    }

    void setFishingState(FishingState fishingState) {
        this.fishingState = fishingState;
    }

    void clearFishingState() {
        fishingState = null;
    }

    CookingState getCookingState() {
        return cookingState;
    }

    void setCookingState(CookingState cookingState) {
        this.cookingState = cookingState;
    }

    void clearCookingState() {
        cookingState = null;
    }

    CraftingState getCraftingState() {
        return craftingState;
    }

    void setCraftingState(CraftingState craftingState) {
        this.craftingState = craftingState;
    }

    void clearCraftingState() {
        craftingState = null;
    }

    PrayerOfferingState getPrayerOfferingState() {
        return prayerOfferingState;
    }

    void setPrayerOfferingState(PrayerOfferingState prayerOfferingState) {
        this.prayerOfferingState = prayerOfferingState;
    }

    void clearPrayerOfferingState() {
        prayerOfferingState = null;
    }

    RunecraftingState getRunecraftingState() {
        return runecraftingState;
    }

    void setRunecraftingState(RunecraftingState runecraftingState) {
        this.runecraftingState = runecraftingState;
    }

    void clearRunecraftingState() {
        runecraftingState = null;
    }

    InteractionAnchorState getInteractionAnchorState() {
        return interactionAnchorState;
    }

    void setInteractionAnchorState(InteractionAnchorState interactionAnchorState) {
        this.interactionAnchorState = interactionAnchorState;
    }

    void clearInteractionAnchorState() {
        interactionAnchorState = null;
    }

    PyramidPlunderPlayerState getPyramidPlunderState() {
        return pyramidPlunderState;
    }

    void setPyramidPlunderState(PyramidPlunderPlayerState pyramidPlunderState) {
        this.pyramidPlunderState = pyramidPlunderState;
    }

    void clearPyramidPlunderState() {
        pyramidPlunderState = null;
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
        if (handle != null) {
            handle.cancel();
        }
    }

    void clearActiveActionState() {
        activeActionHandle = null;
        activeActionType = null;
        actionStartedCycle = 0L;
        activeActionCancelReason = null;
    }

    PlayerActionCancelReason getActiveActionCancelReason() {
        return activeActionCancelReason;
    }

    void setActiveActionCancelReason(PlayerActionCancelReason activeActionCancelReason) {
        this.activeActionCancelReason = activeActionCancelReason;
    }

    PlayerActionCancelReason getLastActionCancelReason() {
        return lastActionCancelReason;
    }

    void setLastActionCancelReason(PlayerActionCancelReason lastActionCancelReason) {
        this.lastActionCancelReason = lastActionCancelReason;
    }

    long getLastActionCancelCycle() {
        return lastActionCancelCycle;
    }

    void setLastActionCancelCycle(long lastActionCancelCycle) {
        this.lastActionCancelCycle = lastActionCancelCycle;
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

    CombatCancellationReason getCombatCancellationReason() {
        return combatCancellationReason;
    }

    void setCombatCancellationReason(CombatCancellationReason combatCancellationReason) {
        this.combatCancellationReason = combatCancellationReason;
    }

    void clearCombatCancellationReason() {
        combatCancellationReason = null;
    }

    long getCombatLogoutLockUntilCycle() {
        return combatLogoutLockUntilCycle;
    }

    void setCombatLogoutLockUntilCycle(long combatLogoutLockUntilCycle) {
        this.combatLogoutLockUntilCycle = combatLogoutLockUntilCycle;
    }

    long getLastBlockAnimationCycle() {
        return lastBlockAnimationCycle;
    }

    void setLastBlockAnimationCycle(long lastBlockAnimationCycle) {
        this.lastBlockAnimationCycle = lastBlockAnimationCycle;
    }

    DeathTaskState getDeathTaskState() {
        return deathTaskState;
    }

    void setDeathTaskState(DeathTaskState deathTaskState) {
        this.deathTaskState = deathTaskState;
    }

    void clearDeathTaskState() {
        deathTaskState = null;
    }

    ActiveSmithingSelection getActiveSmithingSelection() {
        return activeSmithingSelection;
    }

    void setActiveSmithingSelection(ActiveSmithingSelection activeSmithingSelection) {
        this.activeSmithingSelection = activeSmithingSelection;
    }

    void clearActiveSmithingSelection() {
        activeSmithingSelection = null;
    }

    SmeltingSelection getSmeltingSelection() {
        return smeltingSelection;
    }

    void setSmeltingSelection(SmeltingSelection smeltingSelection) {
        this.smeltingSelection = smeltingSelection;
    }

    void clearSmeltingSelection() {
        smeltingSelection = null;
    }

    int getPendingSmeltingBarId() {
        return pendingSmeltingBarId;
    }

    void setPendingSmeltingBarId(int pendingSmeltingBarId) {
        this.pendingSmeltingBarId = pendingSmeltingBarId;
    }

    void clearPendingSmeltingBarId() {
        pendingSmeltingBarId = -1;
    }

    PendingProductionSelection getPendingProductionSelection() {
        return pendingProductionSelection;
    }

    void setPendingProductionSelection(PendingProductionSelection pendingProductionSelection) {
        this.pendingProductionSelection = pendingProductionSelection;
    }

    void clearPendingProductionSelection() {
        pendingProductionSelection = null;
    }

    ActiveProductionSelection getActiveProductionSelection() {
        return activeProductionSelection;
    }

    void setActiveProductionSelection(ActiveProductionSelection activeProductionSelection) {
        this.activeProductionSelection = activeProductionSelection;
    }

    void clearActiveProductionSelection() {
        activeProductionSelection = null;
    }

    GameTaskSet<?> getPlayerTaskSet() {
        return playerTaskSet;
    }

    void setPlayerTaskSet(GameTaskSet<?> playerTaskSet) {
        this.playerTaskSet = playerTaskSet;
    }

    void terminatePlayerTasks() {
        cancelActiveAction();
        clearActiveActionState();
        if (playerTaskSet != null) {
            playerTaskSet.terminateTasks();
            playerTaskSet = null;
        }
    }

    long getThrottleUntilCycle(String key) {
        return throttleUntilCycles.getOrDefault(key, 0L);
    }

    void setThrottleUntilCycle(String key, long cycle) {
        if (cycle <= 0L) {
            throttleUntilCycles.remove(key);
            return;
        }
        throttleUntilCycles.put(key, cycle);
    }

    void clearThrottleUntilCycle(String key) {
        throttleUntilCycles.remove(key);
    }
}
