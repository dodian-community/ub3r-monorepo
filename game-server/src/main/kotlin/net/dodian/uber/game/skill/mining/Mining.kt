package net.dodian.uber.game.skill.mining

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
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
import net.dodian.uber.game.api.content.ContentEvents
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.util.Misc

object Mining {
    private const val DRAGON_BOOST_ROLL = 8
    private const val DRAGON_BOOST_MS = 600.0

    sealed class MiningStartResult {
        object Allowed : MiningStartResult()

        data class Blocked(
            val message: String,
            val resetAction: Boolean = false,
        ) : MiningStartResult()
    }

    @JvmStatic
    fun attempt(client: Client, objectId: Int, position: Position): Boolean {
        val rock = MiningData.rockByObjectId[objectId] ?: return false
        return startMining(client, rock, position)
    }

    @JvmStatic
    fun stop(client: Client, fullReset: Boolean) = stopMining(client, fullReset)

    @JvmStatic
    fun canMine(client: Client, rock: MiningRockDef): MiningStartResult {
        if (client.getPositionName(client.position) == Player.positions.TZHAAR) {
            return MiningStartResult.Blocked("You can not mine here or the Tzhaar's will be angry!")
        }
        if (client.getLevel(Skill.MINING) < rock.requiredLevel) {
            return MiningStartResult.Blocked("You need a mining level of ${rock.requiredLevel} to mine this rock")
        }
        if (resolveBestPickaxe(client) == null) {
            return MiningStartResult.Blocked(
                "You need a pickaxe in which you got the required mining level for.",
                resetAction = true,
            )
        }
        return MiningStartResult.Allowed
    }

    @JvmStatic
    fun startMining(client: Client, rock: MiningRockDef, position: Position): Boolean {
        ContentActions.cancel(
            player = client,
            reason = PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            resetCompatibilityState = false,
        )
        stopMiningInternal(client, MiningStopReason.USER_INTERRUPT)

        if (client.fletchingState != null || client.isFiremaking || client.craftingState?.mode == net.dodian.uber.game.skill.crafting.CraftingMode.SHAFTING) {
            client.resetAction()
        }

        when (val result = canMine(client, rock)) {
            is MiningStartResult.Allowed -> Unit
            is MiningStartResult.Blocked -> {
                if (result.resetAction) {
                    client.resetAction()
                }
                client.sendMessage(result.message)
                return true
            }
        }

        val pickaxe = resolveBestPickaxe(client) ?: return true
        client.miningState =
            MiningState(
                rockObjectId = rock.objectIds.first(),
                rockPosition = position.copy(),
                startedCycle = ContentTiming.currentCycle(),
                resourcesGathered = 0,
            )

        ContentEvents.post(MiningStartedEvent(client, rock, position.copy(), pickaxe))

        val actionRequirements =
            RequirementBuilder().apply {
                level(Skill.MINING, rock.requiredLevel, "You need a mining level of ${rock.requiredLevel} to mine this rock")
                inventorySpace(1, "Your inventory is full!")
                tool(
                    skill = Skill.MINING,
                    toolIdsByTier = MiningData.pickaxesDescending.map { it.itemId },
                    requiredLevelByTool = MiningData.pickaxesDescending.associate { it.itemId to it.requiredLevel },
                    message = "You need a pickaxe in which you got the required mining level for.",
                )
                requirement(
                    Requirement { localClient ->
                        if (!isNearRock(localClient, position)) {
                            ValidationResult.failed("You moved too far away.")
                        } else {
                            ValidationResult.ok()
                        }
                    },
                )
            }.build()

        val action =
            gatheringAction("Mining") {
                delay {
                    val state = miningState ?: return@delay 1
                    val activeRock = MiningData.rockByObjectId[state.rockObjectId] ?: return@delay 1
                    val activePickaxe = resolveBestPickaxe(this) ?: return@delay 1
                    computeMiningDelayTicks(this, activeRock, activePickaxe)
                }
                requirements {
                    actionRequirements.forEach { requirement(it) }
                }
                onStart {
                    performAnimation(pickaxe.animationId, 0)
                    sendMessage("You swing your pick at the rock...")
                }
                onCycleSignal {
                    if (isBusy) {
                        return@onCycleSignal CycleSignal.stop(ActionStopReason.BUSY)
                    }
                    val state = miningState ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.INVALID_TARGET)
                    if (MiningData.rockByObjectId[state.rockObjectId] == null) {
                        return@onCycleSignal CycleSignal.stop(ActionStopReason.INVALID_TARGET)
                    }
                    val activePickaxe = resolveBestPickaxe(this)
                        ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.MISSING_TOOL)

                    performAnimation(activePickaxe.animationId, 0)
                    if (!performMiningCycle(this)) {
                        return@onCycleSignal CycleSignal.stop(ActionStopReason.COMPLETED)
                    }
                    CycleSignal.success()
                }
                onStop { reason ->
                    stopMiningInternal(this, mapStopReason(reason))
                }
            }
        if (action.start(client) == null) {
            stopMiningInternal(client, MiningStopReason.INVALID_ROCK)
        }
        return true
    }

    @JvmStatic
    fun stopMining(client: Client, fullReset: Boolean) {
        ContentActions.cancel(
            player = client,
            reason = PlayerActionCancelReason.MANUAL_RESET,
            fullResetAnimation = fullReset,
            resetCompatibilityState = false,
        )
        stopMiningInternal(client, MiningStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopMiningFromReset(client: Client, fullReset: Boolean) {
        ContentActions.cancel(
            player = client,
            reason = PlayerActionCancelReason.MANUAL_RESET,
            fullResetAnimation = fullReset,
            resetCompatibilityState = false,
        )
        stopMiningInternal(client, MiningStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun resolveBestPickaxe(client: Client): PickaxeDef? {
        val miningLevel = client.getLevel(Skill.MINING)
        val equippedWeapon = client.equipment[Equipment.Slot.WEAPON.id]
        return MiningData.pickaxesDescending.firstOrNull { pickaxe ->
            miningLevel >= pickaxe.requiredLevel &&
                (pickaxe.itemId == equippedWeapon || client.playerHasItem(pickaxe.itemId))
        }
    }

    @JvmStatic
    fun computeMiningDelayMs(client: Client, rock: MiningRockDef, pickaxe: PickaxeDef): Long {
        return computeMiningDelayMs(client, rock, pickaxe, Misc.chance(DRAGON_BOOST_ROLL))
    }

    @JvmStatic
    fun computeMiningDelayTicks(client: Client, rock: MiningRockDef, pickaxe: PickaxeDef): Int {
        return ContentTiming.ticksForDurationMs(computeMiningDelayMs(client, rock, pickaxe))
    }

    internal fun computeMiningDelayMs(
        client: Client,
        rock: MiningRockDef,
        pickaxe: PickaxeDef,
        boostRoll: Int,
    ): Long {
        val levelBonus = client.getLevel(Skill.MINING) / 256.0
        val bonus = 1 + pickaxe.speedBonus + levelBonus
        var timer = rock.baseDelayMs.toDouble()
        if (pickaxe.dragonTierBoostEligible && boostRoll == 1) {
            timer -= DRAGON_BOOST_MS
        }
        return (timer / bonus).toLong()
    }

    @JvmStatic
    fun performMiningCycle(client: Client): Boolean {
        val state = client.miningState ?: return false
        val rock = MiningData.rockByObjectId[state.rockObjectId]
            ?: return stopMiningInternal(client, MiningStopReason.INVALID_ROCK)
        val pickaxe =
            resolveBestPickaxe(client)
                ?: run {
                    client.sendMessage("You need a pickaxe in which you got the required mining level for.")
                    return stopMiningInternal(client, MiningStopReason.NO_PICKAXE)
                }

        if (!client.playerHasItem(-1)) {
            client.sendMessage("Your inventory is full!")
            return stopMiningInternal(client, MiningStopReason.FULL_INVENTORY)
        }

        if (rock.oreItemId != 1436) {
            client.sendMessage("You mine some ${client.getItemName(rock.oreItemId).lowercase()}")
        }
        client.addItem(rock.oreItemId, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, rock.oreItemId, 1, client.position.copy(), "Mining")
        ProgressionService.addXp(client, rock.experience, Skill.MINING)
        SkillingRandomEventService.trigger(client, rock.experience)
        if (rock.randomGemEligible) {
            tryAwardRandomGem(client)
        }

        val updatedState = state.copy(resourcesGathered = state.resourcesGathered + 1)
        client.miningState = updatedState
        ContentEvents.post(MiningSuccessEvent(client, rock, rock.oreItemId, rock.experience, client.position.copy()))

        if (updatedState.resourcesGathered >= rock.restThreshold && Misc.chance(20) == 1) {
            client.sendMessage("You take a rest after gathering ${updatedState.resourcesGathered} resources.")
            return stopMiningInternal(client, MiningStopReason.RESTED)
        }

        client.performAnimation(pickaxe.animationId, 0)
        return true
    }

    internal fun tryAwardRandomGem(client: Client): Int? {
        val chance = resolveRandomGemChance(client)
        return tryAwardRandomGem(client, chance, Misc.chance(chance), Misc.random(MiningData.randomGemDropTable.size - 1))
    }

    internal fun resolveRandomGemChance(client: Client): Int {
        return if (client.getItemName(client.equipment[Equipment.Slot.NECK.id]).lowercase().contains("glory")) {
            128
        } else {
            256
        }
    }

    internal fun tryAwardRandomGem(
        client: Client,
        chance: Int,
        roll: Int,
        gemIndex: Int,
    ): Int? {
        if (chance != 128 && chance != 256) {
            return null
        }
        if (client.freeSlots() < 1 || roll != 1) {
            return null
        }
        val gem = MiningData.randomGemDropTable[gemIndex.coerceIn(0, MiningData.randomGemDropTable.lastIndex)]
        client.addItem(gem, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, gem, 1, client.position.copy(), "Mining")
        client.sendMessage("You found a ${client.getItemName(gem).lowercase()} inside the rock.")
        return gem
    }

    private fun stopMiningInternal(
        client: Client,
        reason: MiningStopReason,
    ): Boolean {
        val state = client.miningState
        val rock = state?.let { MiningData.rockByObjectId[it.rockObjectId] }
        val position = state?.rockPosition?.copy()

        client.clearMiningState()

        if (state != null) {
            ContentEvents.post(MiningStoppedEvent(client, rock, position, reason))
        }
        return false
    }

    private fun mapStopReason(reason: ActionStopReason): MiningStopReason =
        when (reason) {
            ActionStopReason.MOVED_AWAY -> MiningStopReason.MOVED_AWAY
            ActionStopReason.DISCONNECTED -> MiningStopReason.DISCONNECTED
            ActionStopReason.BUSY -> MiningStopReason.BUSY
            ActionStopReason.FULL_INVENTORY -> MiningStopReason.FULL_INVENTORY
            ActionStopReason.MISSING_TOOL -> MiningStopReason.NO_PICKAXE
            ActionStopReason.INVALID_TARGET,
            ActionStopReason.REQUIREMENT_FAILED,
            -> MiningStopReason.INVALID_ROCK
            ActionStopReason.COMPLETED -> MiningStopReason.RESTED
            ActionStopReason.USER_INTERRUPT -> MiningStopReason.USER_INTERRUPT
        }

    private fun isNearRock(client: Client, rockPosition: Position): Boolean {
        if (client.position.z != rockPosition.z) {
            return false
        }
        val deltaX = kotlin.math.abs(client.position.x - rockPosition.x)
        val deltaY = kotlin.math.abs(client.position.y - rockPosition.y)
        return (deltaX + deltaY) == 1
    }
}

object MiningSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Mining", skill = Skill.MINING) {
            objectClick(
                preset = PolicyPreset.GATHERING,
                option = 1,
                *MiningData.allRockObjectIds,
            ) { client, objectId, position, _ ->
                Mining.attempt(client, objectId, position)
            }
        }
}
