package net.dodian.uber.game.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionSucceededEvent
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionController
import net.dodian.uber.game.runtime.action.PlayerActionInterruptPolicy
import net.dodian.uber.game.runtime.action.PlayerActionStopResult
import net.dodian.uber.game.runtime.action.PlayerActionType
import net.dodian.uber.game.runtime.interaction.ObjectInteractionDistance
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.skills.core.ActionStopReason
import net.dodian.utilities.Misc

object WoodcuttingService {
    private const val INITIAL_SWING_DELAY_MS = 600L
    private const val SWING_REPEAT_DELAY_MS = 1800L
    private const val DRAGON_BOOST_ROLL = 8
    private const val DRAGON_BOOST_MS = 600.0

    @JvmStatic
    fun startWoodcutting(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        val tree = WoodcuttingData.treeByObjectId[objectId] ?: return false
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)

        if (client.fletchings || client.isFiremaking || client.shafting) {
            client.resetAction()
        }

        val axe = resolveBestAxe(client)
        if (axe == null) {
            client.send(SendMessage("You need an axe in which you got the required woodcutting level for."))
            client.resetAction()
            return true
        }
        if (client.getLevel(Skill.WOODCUTTING) < tree.requiredLevel) {
            client.send(SendMessage("You need a woodcutting level of ${tree.requiredLevel} to cut this tree."))
            return true
        }
        if (!client.playerHasItem(-1)) {
            client.send(SendMessage("You got full inventory!"))
            return true
        }
        if (!isWithinTreeBoundaryDistance(client, objectId, position, obj)) {
            client.send(SendMessage("You moved too far away from the tree."))
            return true
        }
        val startedCycle = GameCycleClock.currentCycle()
        client.woodcuttingState =
            WoodcuttingState(
                treeObjectId = objectId,
                treePosition = position.copy(),
                objectData = obj,
                startedCycle = startedCycle,
                nextSwingAnimationCycle = startedCycle + GameCycleClock.ticksForDurationMs(INITIAL_SWING_DELAY_MS),
                nextResourceCycle = startedCycle + computeInitialWoodcuttingDelayTicks(client, tree, axe),
                resourcesGathered = 0,
            )
        client.requestAnim(axe.animationId, 0)
        client.send(SendMessage("You swing your axe at the tree..."))
        GameEventBus.post(SkillingActionStartedEvent(client, "Woodcutting"))
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.WOODCUTTING,
            interruptPolicy = PlayerActionInterruptPolicy(cancelOnMovement = true),
            onStop = { player, result ->
                stopWoodcuttingInternal(player, mapStopReason(result))
            },
        ) {
            while (true) {
                cancellationReason()?.let { return@start }
                if (!advanceWoodcutting(player)) {
                    return@start
                }
                wait(1)
            }
        }
        return true
    }

    @JvmStatic
    fun stopWoodcutting(client: Client, fullReset: Boolean) {
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.MANUAL_RESET,
            fullResetAnimation = fullReset,
            resetCompatibilityState = false,
        )
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopWoodcuttingFromReset(client: Client, fullReset: Boolean) {
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun resolveBestAxe(client: Client): AxeDef? {
        val level = client.getLevel(Skill.WOODCUTTING)
        val equippedWeapon = client.equipment[Equipment.Slot.WEAPON.id]
        return WoodcuttingData.axesDescending.firstOrNull { axe ->
            level >= axe.requiredLevel && (equippedWeapon == axe.itemId || client.playerHasItem(axe.itemId))
        }
    }

    @JvmStatic
    fun computeWoodcuttingDelayMs(client: Client, tree: TreeDef, axe: AxeDef): Long {
        val levelBonus = client.getLevel(Skill.WOODCUTTING) / 256.0
        val bonus = 1 + axe.speedBonus + levelBonus
        var timer = tree.baseDelayMs.toDouble()
        if (axe.dragonTierBoostEligible && Misc.chance(DRAGON_BOOST_ROLL) == 1) {
            timer -= DRAGON_BOOST_MS
        }
        return (timer / bonus).toLong()
    }

    @JvmStatic
    fun computeWoodcuttingDelayTicks(client: Client, tree: TreeDef, axe: AxeDef): Int {
        return GameCycleClock.ticksForDurationMs(computeWoodcuttingDelayMs(client, tree, axe))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun stopWoodcuttingInternal(
        client: Client,
        reason: ActionStopReason,
    ) {
        val hadWoodcutting = client.woodcuttingState != null || client.activeActionType == PlayerActionType.WOODCUTTING
        client.clearWoodcuttingState()
        if (hadWoodcutting) {
            GameEventBus.post(SkillingActionStoppedEvent(client, "Woodcutting", reason))
        }
    }

    private fun advanceWoodcutting(client: Client): Boolean {
        val state = client.woodcuttingState ?: return false
        val activeTree = WoodcuttingData.treeByObjectId[state.treeObjectId] ?: return stopWoodcuttingInternal(client, ActionStopReason.INVALID_TARGET).let { false }
        if (client.isBusy) {
            return stopWoodcuttingInternal(client, ActionStopReason.BUSY).let { false }
        }
        if (!isWithinTreeBoundaryDistance(client, state.treeObjectId, state.treePosition, state.objectData)) {
            client.send(SendMessage("You moved too far away from the tree."))
            return stopWoodcuttingInternal(client, ActionStopReason.MOVED_AWAY).let { false }
        }

        val activeAxe = resolveBestAxe(client)
        if (activeAxe == null) {
            client.send(SendMessage("You need an axe in which you got the required woodcutting level for."))
            return stopWoodcuttingInternal(client, ActionStopReason.MISSING_TOOL).let { false }
        }

        if (client.getLevel(Skill.WOODCUTTING) < activeTree.requiredLevel) {
            client.send(SendMessage("You need a woodcutting level of ${activeTree.requiredLevel} to cut this tree."))
            return stopWoodcuttingInternal(client, ActionStopReason.REQUIREMENT_FAILED).let { false }
        }

        val cycle = GameCycleClock.currentCycle()
        if (cycle >= state.nextSwingAnimationCycle) {
            client.requestAnim(activeAxe.animationId, 0)
            client.woodcuttingState =
                state.copy(nextSwingAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(SWING_REPEAT_DELAY_MS))
        }

        if (cycle >= state.nextResourceCycle) {
            if (!client.playerHasItem(-1)) {
                client.send(SendMessage("Your inventory is full!"))
                return stopWoodcuttingInternal(client, ActionStopReason.FULL_INVENTORY).let { false }
            }

            client.send(SendMessage("You cut some ${client.GetItemName(activeTree.logItemId).lowercase()}"))
            client.addItem(activeTree.logItemId, 1)
            client.checkItemUpdate()
            ItemLog.playerGathering(client, activeTree.logItemId, 1, client.position.copy(), "Woodcutting")
            client.giveExperience(activeTree.experience, Skill.WOODCUTTING)
            client.triggerRandom(activeTree.experience)
            client.requestAnim(activeAxe.animationId, 0)

            val gathered = state.resourcesGathered + 1
            client.woodcuttingState =
                state.copy(
                    resourcesGathered = gathered,
                    nextSwingAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(SWING_REPEAT_DELAY_MS),
                    nextResourceCycle = cycle + computeWoodcuttingDelayTicks(client, activeTree, activeAxe),
                )
            GameEventBus.post(SkillingActionCycleEvent(client, "Woodcutting"))
            GameEventBus.post(SkillingActionSucceededEvent(client, "Woodcutting"))

            if (gathered >= 4 && Misc.chance(20) == 1) {
                client.send(SendMessage("You take a rest after gathering $gathered resources."))
                return stopWoodcuttingInternal(client, ActionStopReason.COMPLETED).let { false }
            }
        }

        return true
    }

    private fun mapStopReason(result: PlayerActionStopResult): ActionStopReason =
        when (result) {
            PlayerActionStopResult.Completed -> ActionStopReason.USER_INTERRUPT
            is PlayerActionStopResult.Cancelled ->
                when (result.reason) {
                    PlayerActionCancelReason.DISCONNECTED -> ActionStopReason.DISCONNECTED
                    PlayerActionCancelReason.MOVEMENT -> ActionStopReason.MOVED_AWAY
                    else -> ActionStopReason.USER_INTERRUPT
                }
        }

    private fun isWithinTreeBoundaryDistance(
        client: Client,
        objectId: Int,
        treePosition: Position,
        objectData: GameObjectData?,
    ): Boolean {
        val playerPlanePosition = Position(treePosition.x, treePosition.y, client.position.z)
        val resolved =
            ObjectInteractionDistance.resolveDistancePosition(
                client,
                playerPlanePosition,
                objectId,
                objectData,
                null,
                ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL,
        )
        return resolved != null
    }

    private fun computeInitialWoodcuttingDelayTicks(
        client: Client,
        tree: TreeDef,
        axe: AxeDef,
    ): Int {
        val remainingDelayMs = computeWoodcuttingDelayMs(client, tree, axe) - INITIAL_SWING_DELAY_MS
        return if (remainingDelayMs <= 0L) 0 else GameCycleClock.ticksForDurationMs(remainingDelayMs)
    }
}
