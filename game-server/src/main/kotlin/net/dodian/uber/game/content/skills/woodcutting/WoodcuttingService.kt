package net.dodian.uber.game.content.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.requirements.Requirement
import net.dodian.uber.game.content.skills.core.requirements.ValidationResult
import net.dodian.uber.game.content.skills.core.runtime.ActionStopReason
import net.dodian.uber.game.content.skills.core.runtime.RunningGatheringAction
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.content.skills.core.runtime.gatheringAction
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionInterruptEvent
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.systems.interaction.ObjectInteractionDistance
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.utilities.Misc
import java.util.Collections
import java.util.WeakHashMap

object WoodcuttingService {
    private const val DRAGON_BOOST_ROLL = 8
    private const val DRAGON_BOOST_MS = 600.0

    private val activeTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningGatheringAction>())

    @JvmStatic
    fun startWoodcutting(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        val tree = WoodcuttingDefinitions.treeByObjectId[objectId] ?: return false
        stopActiveTask(client, ActionStopReason.USER_INTERRUPT)
        stopWoodcuttingInternal(client, ActionStopReason.USER_INTERRUPT)

        if (client.fletchingState != null || client.isFiremaking || client.craftingState?.mode == net.dodian.uber.game.content.skills.crafting.CraftingMode.SHAFTING) {
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

        client.woodcuttingState =
            WoodcuttingState(
                treeObjectId = objectId,
                treePosition = position.copy(),
                objectData = obj,
                startedCycle = ContentTiming.currentCycle(),
                resourcesGathered = 0,
            )

        val action =
            gatheringAction("Woodcutting") {
                delay {
                    val state = woodcuttingState ?: return@delay 1
                    val activeTree = WoodcuttingDefinitions.treeByObjectId[state.treeObjectId] ?: return@delay 1
                    val activeAxe = resolveBestAxe(this) ?: return@delay 1
                    computeWoodcuttingDelayTicks(this, activeTree, activeAxe)
                }
                requirements {
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
                }
                onStart {
                    requestAnim(axe.animationId, 0)
                    send(SendMessage("You swing your axe at the tree..."))
                }
                onCycleWhile {
                    val state = woodcuttingState ?: return@onCycleWhile false
                    val activeTree = WoodcuttingDefinitions.treeByObjectId[state.treeObjectId]
                        ?: return@onCycleWhile stopWoodcuttingInternal(this, ActionStopReason.INVALID_TARGET).let { false }
                    val activeAxe = resolveBestAxe(this)
                        ?: return@onCycleWhile stopWoodcuttingInternal(this, ActionStopReason.MISSING_TOOL).let { false }

                    requestAnim(activeAxe.animationId, 0)
                    send(SendMessage("You cut some ${GetItemName(activeTree.logItemId).lowercase()}"))
                    addItem(activeTree.logItemId, 1)
                    checkItemUpdate()
                    ItemLog.playerGathering(this, activeTree.logItemId, 1, position.copy(), "Woodcutting")
                    SkillProgressionService.gainXp(this, activeTree.experience, Skill.WOODCUTTING)
                    SkillingRandomEventService.trigger(this, activeTree.experience)

                    val gathered = state.resourcesGathered + 1
                    woodcuttingState = state.copy(resourcesGathered = gathered)

                    if (gathered >= 4 && Misc.chance(20) == 1) {
                        send(SendMessage("You take a rest after gathering $gathered resources."))
                        stopWoodcuttingInternal(this, ActionStopReason.COMPLETED)
                        return@onCycleWhile false
                    }
                    true
                }
                onStop { reason ->
                    stopWoodcuttingInternal(this, reason)
                }
            }

        val running = action.start(client) ?: return true
        activeTasks[client] = running
        return true
    }

    @JvmStatic
    fun stopWoodcutting(client: Client, fullReset: Boolean) {
        stopActiveTask(client, ActionStopReason.USER_INTERRUPT)
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
        stopActiveTask(client, ActionStopReason.USER_INTERRUPT)
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
        activeTasks.remove(client)
        val hadWoodcutting = client.woodcuttingState != null
        client.clearWoodcuttingState()
        if (hadWoodcutting) {
            GameEventBus.post(SkillingActionStoppedEvent(client, "Woodcutting", reason))
            GameEventBus.post(SkillActionInterruptEvent(client, "Woodcutting", reason))
        }
    }

    private fun stopActiveTask(client: Client, reason: ActionStopReason) {
        activeTasks.remove(client)?.cancel(reason)
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
