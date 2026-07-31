package net.dodian.uber.game.model.entity.player

import java.util.concurrent.ConcurrentHashMap
import net.dodian.uber.game.engine.lifecycle.DeathTaskState
import net.dodian.uber.game.engine.scheduler.QueueTaskHandle
import net.dodian.uber.game.engine.systems.action.ActiveProductionSelection
import net.dodian.uber.game.engine.systems.action.PendingProductionSelection
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.action.PlayerActionType
import net.dodian.uber.game.engine.systems.combat.CombatCancellationReason
import net.dodian.uber.game.engine.systems.combat.CombatCooldownState
import net.dodian.uber.game.engine.systems.combat.CombatEngagementState
import net.dodian.uber.game.engine.systems.combat.CombatTargetState
import net.dodian.uber.game.engine.systems.interaction.ActiveInteraction
import net.dodian.uber.game.engine.systems.interaction.InteractionAnchorState
import net.dodian.uber.game.engine.systems.interaction.InteractionIntent
import net.dodian.uber.game.engine.sync.player.PlayerSyncInvariantValidator
import net.dodian.uber.game.engine.tasking.GameTaskSet
import net.dodian.uber.game.api.plugin.skills.PendingSkillMulti

/**
 * Content-owned, per-player runtime state. This intentionally has no protocol
 * dependency and is the Kotlin home for skill/action/session state formerly
 * embedded in Player's Java interaction holder.
 */
class PlayerContentRuntimeState {
    @Volatile private var pendingInputState = PendingInputState.NONE
    private val pluginAttributes = ConcurrentHashMap<String, Any>()
    @Volatile private var pendingInteraction: InteractionIntent? = null
    @Volatile private var activeInteraction: ActiveInteraction? = null
    @Volatile private var interactionEarliestCycle = 0L
    @Volatile private var interactionTaskHandle: QueueTaskHandle? = null
    @Volatile private var farmDebugTaskHandle: QueueTaskHandle? = null
    @Volatile private var activeActionHandle: QueueTaskHandle? = null
    @Volatile private var activeActionType: PlayerActionType? = null
    @Volatile private var actionStartedCycle = 0L
    @Volatile private var activeActionCancelReason: PlayerActionCancelReason? = null
    @Volatile private var lastActionCancelReason: PlayerActionCancelReason? = null
    @Volatile private var lastActionCancelCycle = 0L
    @Volatile private var combatTargetState: CombatTargetState? = null
    @Volatile private var combatEngagementState: CombatEngagementState? = null
    @Volatile private var combatCooldownState: CombatCooldownState? = null
    @Volatile private var attackStartDedupeState: AttackStartDedupeState? = null
    @Volatile private var combatCancellationReason: CombatCancellationReason? = null
    @Volatile private var combatLogoutLockUntilCycle = 0L
    @Volatile private var combatAttackEnabled = true
    @Volatile private var combatUsingBow = false
    @Volatile private var lastBlockAnimationCycle = -1L
    // Dev/staging-only diagnostic scratch: previous tick's local-list snapshot for
    // PlayerSyncInvariantValidator (see WorldSynchronizationService, gated on serverDebugMode).
    // Lives on the player object itself (not an external map) so a new login never sees a
    // stale snapshot left behind by a prior session that happened to reuse the same slot.
    @Volatile private var previousSyncSnapshot: PlayerSyncInvariantValidator.ViewerLocalSnapshot? = null
    @Volatile private var deathTaskState: DeathTaskState? = null
    @Volatile private var pendingProductionSelection: PendingProductionSelection? = null
    @Volatile private var activeProductionSelection: ActiveProductionSelection? = null
    @Volatile private var playerTaskSet: GameTaskSet<*>? = null
    @Volatile private var interactionAnchorState: InteractionAnchorState? = null
    @Volatile private var movementLockState: MovementLockState? = null
    @Volatile private var skillingEventState: SkillingEventState? = null
    @Volatile private var playerPotatoState: PlayerPotatoState? = null
    @Volatile private var activeSkillSessionKey: String? = null
    @Volatile private var activeSkillSessionStartedCycle = 0L
    @Volatile private var pendingSkillMulti: PendingSkillMulti? = null
    @Volatile private var selectedSkillGuideSkillId = -1
    @Volatile private var effectsPeriodicDirtyAtMillis = 0L
    @Volatile private var lastDoorToggleCycle = 0L
    @Volatile private var resourcesGathered = 0
    @Volatile private var pickupWanted = false
    private val economyRuntimeState = EconomyRuntimeState()
    private val throttleUntilCycles = ConcurrentHashMap<String, Long>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPluginAttribute(key: String): T? = pluginAttributes[key] as? T
    fun putPluginAttribute(key: String, value: Any) { pluginAttributes[key] = value }
    fun removePluginAttribute(key: String) { pluginAttributes.remove(key) }
    fun clearPluginAttributes() { pluginAttributes.clear() }
    fun getEconomyRuntimeState() = economyRuntimeState
    fun getPendingInputState() = pendingInputState
    fun setPendingInputState(value: PendingInputState) { pendingInputState = value }
    fun clearPendingInputState() { pendingInputState = PendingInputState.NONE }

    fun getPendingInteraction() = pendingInteraction
    fun setPendingInteraction(value: InteractionIntent?) { pendingInteraction = value }
    fun getActiveInteraction() = activeInteraction
    fun setActiveInteraction(value: ActiveInteraction?) { activeInteraction = value }
    fun getInteractionEarliestCycle() = interactionEarliestCycle
    fun setInteractionEarliestCycle(value: Long) { interactionEarliestCycle = value }

    fun getInteractionTaskHandle() = interactionTaskHandle
    fun setInteractionTaskHandle(value: QueueTaskHandle?) { interactionTaskHandle = value }
    fun cancelInteractionTask() { interactionTaskHandle.cancelAndClear { interactionTaskHandle = null } }
    fun getFarmDebugTaskHandle() = farmDebugTaskHandle
    fun setFarmDebugTaskHandle(value: QueueTaskHandle?) { farmDebugTaskHandle = value }
    fun cancelFarmDebugTask() { farmDebugTaskHandle.cancelAndClear { farmDebugTaskHandle = null } }
    fun getInteractionAnchorState() = interactionAnchorState
    fun setInteractionAnchorState(value: InteractionAnchorState?) { interactionAnchorState = value }
    fun clearInteractionAnchorState() { interactionAnchorState = null }
    fun getMovementLockState() = movementLockState
    fun setMovementLockState(value: MovementLockState?) { movementLockState = value }
    fun clearMovementLockState() { movementLockState = null }
    fun getSkillingEventState() = skillingEventState
    fun setSkillingEventState(value: SkillingEventState?) { skillingEventState = value }
    fun getPlayerPotatoState() = playerPotatoState
    fun setPlayerPotatoState(value: PlayerPotatoState?) { playerPotatoState = value }
    fun clearPlayerPotatoState() { playerPotatoState = null }

    fun getActiveSkillSessionKey() = activeSkillSessionKey
    fun setActiveSkillSession(key: String?, startedCycle: Long) { activeSkillSessionKey = key; activeSkillSessionStartedCycle = startedCycle }
    fun getActiveSkillSessionStartedCycle() = activeSkillSessionStartedCycle
    fun clearActiveSkillSession() { activeSkillSessionKey = null; activeSkillSessionStartedCycle = 0L }
    fun getPendingSkillMulti() = pendingSkillMulti
    fun setPendingSkillMulti(value: PendingSkillMulti?) { pendingSkillMulti = value }
    fun clearPendingSkillMulti() { pendingSkillMulti = null }
    fun getSelectedSkillGuideSkillId() = selectedSkillGuideSkillId
    fun setSelectedSkillGuideSkillId(value: Int) { selectedSkillGuideSkillId = value }
    fun clearSelectedSkillGuideSkillId() { selectedSkillGuideSkillId = -1 }
    fun getEffectsPeriodicDirtyAtMillis() = effectsPeriodicDirtyAtMillis
    fun setEffectsPeriodicDirtyAtMillis(value: Long) { effectsPeriodicDirtyAtMillis = value }
    fun getLastDoorToggleCycle() = lastDoorToggleCycle
    fun setLastDoorToggleCycle(value: Long) { lastDoorToggleCycle = value }
    fun getResourcesGathered() = resourcesGathered
    fun setResourcesGathered(value: Int) { resourcesGathered = value }
    fun isPickupWanted() = pickupWanted
    fun setPickupWanted(value: Boolean) { pickupWanted = value }
    fun getActiveActionHandle() = activeActionHandle
    fun setActiveActionHandle(value: QueueTaskHandle?) { activeActionHandle = value }
    fun getActiveActionType() = activeActionType
    fun setActiveActionType(value: PlayerActionType?) { activeActionType = value }
    fun getActionStartedCycle() = actionStartedCycle
    fun setActionStartedCycle(value: Long) { actionStartedCycle = value }
    fun cancelActiveAction() { activeActionHandle?.cancel() }
    fun clearActiveActionState() { activeActionHandle = null; activeActionType = null; actionStartedCycle = 0L; activeActionCancelReason = null }
    fun getActiveActionCancelReason() = activeActionCancelReason
    fun setActiveActionCancelReason(value: PlayerActionCancelReason?) { activeActionCancelReason = value }
    fun getLastActionCancelReason() = lastActionCancelReason
    fun setLastActionCancelReason(value: PlayerActionCancelReason?) { lastActionCancelReason = value }
    fun getLastActionCancelCycle() = lastActionCancelCycle
    fun setLastActionCancelCycle(value: Long) { lastActionCancelCycle = value }

    fun getCombatTargetState() = combatTargetState
    fun setCombatTargetState(value: CombatTargetState?) { combatTargetState = value }
    fun clearCombatTargetState() { combatTargetState = null }
    fun getCombatEngagementState() = combatEngagementState
    fun setCombatEngagementState(value: CombatEngagementState?) { combatEngagementState = value }
    fun clearCombatEngagementState() { combatEngagementState = null }
    fun getCombatCooldownState() = combatCooldownState
    fun setCombatCooldownState(value: CombatCooldownState?) { combatCooldownState = value }
    fun clearCombatCooldownState() { combatCooldownState = null }
    fun getAttackStartDedupeState() = attackStartDedupeState
    fun setAttackStartDedupeState(value: AttackStartDedupeState?) { attackStartDedupeState = value }
    fun clearAttackStartDedupeState() { attackStartDedupeState = null }
    fun getCombatCancellationReason() = combatCancellationReason
    fun setCombatCancellationReason(value: CombatCancellationReason?) { combatCancellationReason = value }
    fun clearCombatCancellationReason() { combatCancellationReason = null }
    fun getCombatLogoutLockUntilCycle() = combatLogoutLockUntilCycle
    fun setCombatLogoutLockUntilCycle(value: Long) { combatLogoutLockUntilCycle = value }
    fun isCombatAttackEnabled() = combatAttackEnabled
    fun setCombatAttackEnabled(value: Boolean) { combatAttackEnabled = value }
    fun isCombatUsingBow() = combatUsingBow
    fun setCombatUsingBow(value: Boolean) { combatUsingBow = value }
    fun getLastBlockAnimationCycle() = lastBlockAnimationCycle
    fun setLastBlockAnimationCycle(value: Long) { lastBlockAnimationCycle = value }
    fun getPreviousSyncSnapshot() = previousSyncSnapshot
    fun setPreviousSyncSnapshot(value: PlayerSyncInvariantValidator.ViewerLocalSnapshot?) { previousSyncSnapshot = value }
    fun getDeathTaskState() = deathTaskState
    fun setDeathTaskState(value: DeathTaskState?) { deathTaskState = value }
    fun clearDeathTaskState() { deathTaskState = null }

    fun getPendingProductionSelection() = pendingProductionSelection
    fun setPendingProductionSelection(value: PendingProductionSelection?) { pendingProductionSelection = value }
    fun clearPendingProductionSelection() { pendingProductionSelection = null }
    fun getActiveProductionSelection() = activeProductionSelection
    fun setActiveProductionSelection(value: ActiveProductionSelection?) { activeProductionSelection = value }
    fun clearActiveProductionSelection() { activeProductionSelection = null }
    fun getPlayerTaskSet() = playerTaskSet
    fun setPlayerTaskSet(value: GameTaskSet<*>?) { playerTaskSet = value }
    fun terminatePlayerTasks() {
        cancelActiveAction(); clearActiveActionState(); clearActiveSkillSession(); clearPendingSkillMulti()
        playerTaskSet?.terminateTasks(); playerTaskSet = null
    }
    fun getThrottleUntilCycle(key: String) = throttleUntilCycles.getOrDefault(key, 0L)
    fun setThrottleUntilCycle(key: String, cycle: Long) { if (cycle <= 0L) throttleUntilCycles.remove(key) else throttleUntilCycles[key] = cycle }
    fun clearThrottleUntilCycle(key: String) { throttleUntilCycles.remove(key) }

    private inline fun QueueTaskHandle?.cancelAndClear(clear: () -> Unit) { val handle = this; clear(); handle?.cancel() }
}
