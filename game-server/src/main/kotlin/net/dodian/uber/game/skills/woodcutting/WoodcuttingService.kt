package net.dodian.uber.game.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.interaction.ObjectInteractionDistance
import net.dodian.uber.game.security.ItemLog
import net.dodian.uber.game.skills.core.ActionStopReason
import net.dodian.uber.game.skills.core.GatheringTask
import net.dodian.uber.game.skills.core.HasInventorySpaceRequirement
import net.dodian.uber.game.skills.core.HasLevelRequirement
import net.dodian.uber.game.skills.core.Requirement
import net.dodian.uber.game.skills.core.ToolRequirement
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
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT, invokeResetAction = false, fullReset = false)

        if (client.fletchings || client.isFiremaking || client.shafting) {
            client.resetAction()
        }

        val axe = resolveBestAxe(client)
        if (axe == null) {
            client.send(SendMessage("You need an axe in which you got the required woodcutting level for."))
            client.resetAction()
            return true
        }

        val requirements =
            listOf<Requirement>(
                HasLevelRequirement(Skill.WOODCUTTING, tree.requiredLevel, "You need a woodcutting level of ${tree.requiredLevel} to cut this tree."),
                HasInventorySpaceRequirement(1, "You got full inventory!"),
                ToolRequirement(
                    skill = Skill.WOODCUTTING,
                    toolIdsByTier = WoodcuttingData.axesDescending.map { it.itemId },
                    requiredLevelByTool = WoodcuttingData.axesDescending.associate { it.itemId to it.requiredLevel },
                    missingToolMessage = "You need an axe in which you got the required woodcutting level for.",
                ),
            )
        if (!isWithinTreeBoundaryDistance(client, objectId, position, obj)) {
            client.send(SendMessage("You moved too far away from the tree."))
            return true
        }

        val task =
            object : GatheringTask(
                actionName = "Woodcutting",
                client = client,
                cycleDelayTicks = 1,
                requirements = requirements,
            ) {
                override fun onStart() {
                    val now = System.currentTimeMillis()
                    client.lastAction = now - INITIAL_SWING_DELAY_MS
                    client.woodcuttingState =
                        WoodcuttingState(
                            treeObjectId = objectId,
                            treePosition = position.copy(),
                            objectData = obj,
                            startedAtMs = now,
                            lastSwingAnimationAtMs = now + INITIAL_SWING_DELAY_MS,
                            resourcesGathered = 0,
                        )
                    client.requestAnim(axe.animationId, 0)
                    client.send(SendMessage("You swing your axe at the tree..."))
                }

                override fun onTick(): Boolean {
                    val state = client.woodcuttingState
                        ?: run {
                            stop(ActionStopReason.INVALID_TARGET)
                            return false
                        }
                    val activeTree = WoodcuttingData.treeByObjectId[state.treeObjectId]
                        ?: run {
                            stop(ActionStopReason.INVALID_TARGET)
                            return false
                        }
                    if (client.isBusy) {
                        stop(ActionStopReason.BUSY)
                        return false
                    }
                    if (!isWithinTreeBoundaryDistance(client, state.treeObjectId, state.treePosition, state.objectData)) {
                        client.send(SendMessage("You moved too far away from the tree."))
                        stop(ActionStopReason.MOVED_AWAY)
                        return false
                    }

                    val activeAxe = resolveBestAxe(client)
                    if (activeAxe == null) {
                        client.send(SendMessage("You need an axe in which you got the required woodcutting level for."))
                        stop(ActionStopReason.MISSING_TOOL)
                        return false
                    }

                    val now = System.currentTimeMillis()
                    if (now >= state.lastSwingAnimationAtMs) {
                        client.requestAnim(activeAxe.animationId, 0)
                        client.woodcuttingState = state.copy(lastSwingAnimationAtMs = now + SWING_REPEAT_DELAY_MS)
                    }

                    if (now - client.lastAction >= computeWoodcuttingDelayMs(client, activeTree, activeAxe)) {
                        if (!client.playerHasItem(-1)) {
                            client.send(SendMessage("Your inventory is full!"))
                            stop(ActionStopReason.FULL_INVENTORY)
                            return false
                        }

                        client.lastAction = now
                        client.send(SendMessage("You cut some ${client.GetItemName(activeTree.logItemId).lowercase()}"))
                        client.addItem(activeTree.logItemId, 1)
                        client.checkItemUpdate()
                        ItemLog.playerGathering(client, activeTree.logItemId, 1, client.position.copy(), "Woodcutting")
                        client.giveExperience(activeTree.experience, Skill.WOODCUTTING)
                        client.triggerRandom(activeTree.experience)
                        client.requestAnim(activeAxe.animationId, 0)

                        val gathered = state.resourcesGathered + 1
                        client.woodcuttingState = state.copy(resourcesGathered = gathered, lastSwingAnimationAtMs = now + SWING_REPEAT_DELAY_MS)
                        succeedCycle()

                        if (gathered >= 4 && Misc.chance(20) == 1) {
                            client.send(SendMessage("You take a rest after gathering $gathered resources."))
                            stop(ActionStopReason.COMPLETED)
                            return false
                        }
                    }

                    return true
                }

                override fun onStop(reason: ActionStopReason) {
                    stopWoodcuttingInternal(client, reason, invokeResetAction = reason == ActionStopReason.BUSY || reason == ActionStopReason.FULL_INVENTORY || reason == ActionStopReason.COMPLETED, fullReset = true)
                }
            }

        return task.start(onHandle = { client.woodcuttingTaskHandle = it })
    }

    @JvmStatic
    fun stopWoodcutting(client: Client, fullReset: Boolean) {
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT, invokeResetAction = false, fullReset = fullReset)
    }

    @JvmStatic
    fun stopWoodcuttingFromReset(client: Client, fullReset: Boolean) {
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT, invokeResetAction = false, fullReset = fullReset)
    }

    @JvmStatic
    fun resolveBestAxe(client: Client): AxeDef? {
        val level = client.getLevel(Skill.WOODCUTTING)
        val equippedWeapon = client.getEquipment()[Equipment.Slot.WEAPON.id]
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

    private fun stopWoodcuttingInternal(
        client: Client,
        reason: ActionStopReason,
        invokeResetAction: Boolean,
        fullReset: Boolean,
    ) {
        client.cancelWoodcuttingTask()
        client.clearWoodcuttingState()
        if (invokeResetAction) {
            client.resetAction(fullReset)
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
}
