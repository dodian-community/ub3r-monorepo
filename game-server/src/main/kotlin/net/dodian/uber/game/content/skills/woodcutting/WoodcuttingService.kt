package net.dodian.uber.game.content.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.systems.skills.requirements.Requirement
import net.dodian.uber.game.systems.skills.requirements.RequirementBuilder
import net.dodian.uber.game.systems.skills.requirements.ValidationResult
import net.dodian.uber.game.systems.skills.ActionStopReason
import net.dodian.uber.game.systems.skills.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.systems.action.PlayerActionType
import net.dodian.uber.game.systems.action.dsl.playerAction
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.systems.interaction.ObjectInteractionDistance
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.utilities.Misc

object WoodcuttingService {
    private const val DRAGON_BOOST_ROLL = 8
    private const val DRAGON_BOOST_MS = 600.0

    @JvmStatic
    fun startWoodcutting(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        val tree = WoodcuttingDefinitions.treeByObjectId[objectId] ?: return false
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            resetCompatibilityState = false,
        )
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)

        if (client.fletchingState != null || client.isFiremaking || client.craftingState?.mode == net.dodian.uber.game.content.skills.crafting.CraftingMode.SHAFTING) {
            client.resetAction()
        }

        val axe = resolveBestAxe(client)
        if (axe == null) {
            client.sendMessage("You need an axe in which you got the required woodcutting level for.")
            client.resetAction()
            return true
        }
        if (client.getLevel(Skill.WOODCUTTING) < tree.requiredLevel) {
            client.sendMessage("You need a woodcutting level of ${tree.requiredLevel} to cut this tree.")
            return true
        }
        if (!client.playerHasItem(-1)) {
            client.sendMessage("You got full inventory!")
            return true
        }
        if (!isWithinTreeBoundaryDistance(client, objectId, position, obj)) {
            client.sendMessage("You moved too far away from the tree.")
            return true
        }

        client.woodcuttingState =
            WoodcuttingState(
                treeObjectId = objectId,
                treePosition = position.copy(),
                objectData = obj,
                startedCycle = ContentTiming.currentCycle(),
                resourcesGathered = 0,
            )

        val requirements =
            RequirementBuilder().apply {
                level(Skill.WOODCUTTING, tree.requiredLevel, "You need a woodcutting level of ${tree.requiredLevel} to cut this tree.")
                inventorySpace(1, "Your inventory is full!")
                tool(
                    skill = Skill.WOODCUTTING,
                    toolIdsByTier = WoodcuttingDefinitions.axesDescending.map { it.itemId },
                    requiredLevelByTool = WoodcuttingDefinitions.axesDescending.associate { it.itemId to it.requiredLevel },
                    message = "You need an axe in which you got the required woodcutting level for.",
                )
                requirement(
                    Requirement { localClient ->
                        if (!isWithinTreeBoundaryDistance(localClient, objectId, position, obj)) {
                            ValidationResult.failed("You moved too far away from the tree.")
                        } else {
                            ValidationResult.ok()
                        }
                    },
                )
            }.build()

        playerAction(
            player = client,
            type = PlayerActionType.WOODCUTTING,
            actionName = "Woodcutting",
            onStart = {
                player.performAnimation(axe.animationId, 0)
                player.sendMessage("You swing your axe at the tree...")
            },
            onStop = { stoppedPlayer, reason ->
                stopWoodcuttingInternal(stoppedPlayer, reason)
            },
        ) {
            while (true) {
                ensureAll(requirements)
                val state = player.woodcuttingState ?: stop(ActionStopReason.INVALID_TARGET)
                val activeTree = WoodcuttingDefinitions.treeByObjectId[state.treeObjectId]
                    ?: stop(ActionStopReason.INVALID_TARGET)
                val activeAxe = resolveBestAxe(player)
                    ?: stop(ActionStopReason.MISSING_TOOL)

                player.performAnimation(activeAxe.animationId, 0)
                player.sendMessage("You cut some ${player.getItemName(activeTree.logItemId).lowercase()}")
                player.addItem(activeTree.logItemId, 1)
                player.checkItemUpdate()
                ItemLog.playerGathering(player, activeTree.logItemId, 1, position.copy(), "Woodcutting")
                ProgressionService.addXp(player, activeTree.experience, Skill.WOODCUTTING)
                SkillingRandomEventService.trigger(player, activeTree.experience)
                emitCycleSuccess("Woodcutting")

                val gathered = state.resourcesGathered + 1
                player.woodcuttingState = state.copy(resourcesGathered = gathered)
                if (gathered >= 4 && Misc.chance(20) == 1) {
                    player.sendMessage("You take a rest after gathering $gathered resources.")
                    stop(ActionStopReason.COMPLETED)
                }
                waitTicks(computeWoodcuttingDelayTicks(player, activeTree, activeAxe))
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
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.MANUAL_RESET,
            fullResetAnimation = fullReset,
            resetCompatibilityState = false,
        )
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun resolveBestAxe(client: Client): AxeDef? {
        val level = client.getLevel(Skill.WOODCUTTING)
        val equippedWeapon = client.equipment[Equipment.Slot.WEAPON.id]
        return WoodcuttingDefinitions.axesDescending.firstOrNull { axe ->
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
        return ContentTiming.ticksForDurationMs(computeWoodcuttingDelayMs(client, tree, axe))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun stopWoodcuttingInternal(
        client: Client,
        reason: ActionStopReason,
    ) {
        client.clearWoodcuttingState()
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
