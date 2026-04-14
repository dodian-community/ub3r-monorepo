package net.dodian.uber.game.skill.woodcutting

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.skill.runtime.requirements.Requirement
import net.dodian.uber.game.skill.runtime.requirements.RequirementBuilder
import net.dodian.uber.game.skill.runtime.requirements.ValidationResult
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.gatheringAction
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.interaction.ObjectInteractionDistance
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.util.Misc

object Woodcutting {
    private const val DRAGON_BOOST_ROLL = 8
    private const val DRAGON_BOOST_MS = 600.0

    @JvmStatic
    fun attempt(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean = startWoodcutting(client, objectId, position, obj)

    @JvmStatic
    fun startWoodcutting(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        val tree = WoodcuttingData.treeByObjectId[objectId] ?: return false
        ContentActions.cancel(
            player = client,
            reason = PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            resetCompatibilityState = false,
        )
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)

        if (client.fletchingState != null || client.isFiremaking || client.craftingState?.mode == net.dodian.uber.game.skill.crafting.CraftingMode.SHAFTING) {
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

        val actionRequirements =
            RequirementBuilder().apply {
                level(Skill.WOODCUTTING, tree.requiredLevel, "You need a woodcutting level of ${tree.requiredLevel} to cut this tree.")
                inventorySpace(1, "Your inventory is full!")
                tool(
                    skill = Skill.WOODCUTTING,
                    toolIdsByTier = WoodcuttingData.axesDescending.map { it.itemId },
                    requiredLevelByTool = WoodcuttingData.axesDescending.associate { it.itemId to it.requiredLevel },
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

        val action =
            gatheringAction("Woodcutting") {
                delay {
                    val state = woodcuttingState ?: return@delay 1
                    val activeTree = WoodcuttingData.treeByObjectId[state.treeObjectId] ?: return@delay 1
                    val activeAxe = resolveBestAxe(this) ?: return@delay 1
                    computeWoodcuttingDelayTicks(this, activeTree, activeAxe)
                }
                requirements {
                    actionRequirements.forEach { requirement(it) }
                }
                onStart {
                    performAnimation(axe.animationId, 0)
                    sendMessage("You swing your axe at the tree...")
                }
                onCycleSignal {
                    val state = woodcuttingState ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.INVALID_TARGET)
                    val activeTree = WoodcuttingData.treeByObjectId[state.treeObjectId]
                        ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.INVALID_TARGET)
                    val activeAxe = resolveBestAxe(this)
                        ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.MISSING_TOOL)

                    performAnimation(activeAxe.animationId, 0)
                    sendMessage("You cut some ${getItemName(activeTree.logItemId).lowercase()}")
                    addItem(activeTree.logItemId, 1)
                    checkItemUpdate()
                    ItemLog.playerGathering(this, activeTree.logItemId, 1, position.copy(), "Woodcutting")
                    ProgressionService.addXp(this, activeTree.experience, Skill.WOODCUTTING)
                    SkillingRandomEventService.trigger(this, activeTree.experience)

                    val gathered = state.resourcesGathered + 1
                    woodcuttingState = state.copy(resourcesGathered = gathered)
                    if (gathered >= 4 && Misc.chance(20) == 1) {
                        sendMessage("You take a rest after gathering $gathered resources.")
                        return@onCycleSignal CycleSignal.stop(ActionStopReason.COMPLETED)
                    }
                    CycleSignal.success()
                }
                onStop { reason ->
                    stopWoodcuttingInternal(this, reason)
                }
            }
        if (action.start(client) == null) {
            stopWoodcuttingInternal(client, ActionStopReason.REQUIREMENT_FAILED)
        }
        return true
    }

    @JvmStatic
    fun stopWoodcutting(client: Client, fullReset: Boolean) {
        ContentActions.cancel(
            player = client,
            reason = PlayerActionCancelReason.MANUAL_RESET,
            fullResetAnimation = fullReset,
            resetCompatibilityState = false,
        )
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopWoodcuttingFromReset(client: Client, fullReset: Boolean) {
        ContentActions.cancel(
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

object WoodcuttingSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Woodcutting", skill = Skill.WOODCUTTING) {
            objectClick(
                preset = PolicyPreset.GATHERING,
                option = 1,
                *WoodcuttingData.allTreeObjectIds,
            ) { client, objectId, position, obj ->
                Woodcutting.attempt(client, objectId, position, obj)
            }
        }
}
