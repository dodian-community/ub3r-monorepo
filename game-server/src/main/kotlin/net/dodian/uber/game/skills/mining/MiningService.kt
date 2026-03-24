package net.dodian.uber.game.skills.mining

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionController
import net.dodian.uber.game.runtime.action.PlayerActionInterruptPolicy
import net.dodian.uber.game.runtime.action.PlayerActionStopResult
import net.dodian.uber.game.runtime.action.PlayerActionType
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.utilities.Misc

object MiningService {
    private const val INITIAL_SWING_DELAY_MS = 600L
    private const val SWING_REPEAT_DELAY_MS = 1800L
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
        stopMiningInternal(client, MiningStopReason.USER_INTERRUPT)

        if (client.fletchingState != null || client.isFiremaking || client.craftingState?.mode == net.dodian.uber.game.skills.crafting.CraftingMode.SHAFTING) {
            client.resetAction()
        }

        when (val result = canMine(client, rock)) {
            is MiningStartResult.Allowed -> Unit
            is MiningStartResult.Blocked -> {
                if (result.resetAction) {
                    client.resetAction()
                }
                client.send(SendMessage(result.message))
                return true
            }
        }

        val pickaxe = resolveBestPickaxe(client) ?: return true
        val startedCycle = GameCycleClock.currentCycle()
        val initialDelayTicks = computeInitialMiningDelayTicks(client, rock, pickaxe)
        client.miningState =
            MiningState(
                rockObjectId = rock.objectIds.first(),
                rockPosition = position.copy(),
                startedCycle = startedCycle,
                nextSwingAnimationCycle = startedCycle + GameCycleClock.ticksForDurationMs(INITIAL_SWING_DELAY_MS),
                nextResourceCycle = startedCycle + initialDelayTicks,
                resourcesGathered = 0,
            )
        client.requestAnim(pickaxe.animationId, 0)
        client.send(SendMessage("You swing your pick at the rock..."))
        GameEventBus.post(MiningStartedEvent(client, rock, position.copy(), pickaxe))
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.MINING,
            interruptPolicy = PlayerActionInterruptPolicy(cancelOnMovement = true),
            onStop = { player, result ->
                stopMiningInternal(player, mapStopReason(result))
            },
        ) {
            while (true) {
                cancellationReason()?.let { return@start }
                if (!advanceMining(player)) {
                    return@start
                }
                wait(1)
            }
        }
        return true
    }

    @JvmStatic
    fun stopMining(client: Client, fullReset: Boolean) {
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.MANUAL_RESET,
            fullResetAnimation = fullReset,
            resetCompatibilityState = false,
        )
        stopMiningInternal(client, MiningStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopMiningFromReset(client: Client, fullReset: Boolean) {
        stopMiningInternal(client, MiningStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun resolveBestPickaxe(client: Client): PickaxeDef? {
        val miningLevel = client.getLevel(Skill.MINING)
        val equippedWeapon = client.equipment[Equipment.Slot.WEAPON.id]
        return MiningDefinitions.pickaxesDescending.firstOrNull { pickaxe ->
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
        return GameCycleClock.ticksForDurationMs(computeMiningDelayMs(client, rock, pickaxe))
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
        val rock = MiningDefinitions.rockByObjectId[state.rockObjectId]
            ?: return stopMiningInternal(client, MiningStopReason.INVALID_ROCK)
        val pickaxe =
            resolveBestPickaxe(client)
                ?: run {
                    client.send(SendMessage("You need a pickaxe in which you got the required mining level for."))
                    return stopMiningInternal(client, MiningStopReason.NO_PICKAXE)
                }

        if (!client.playerHasItem(-1)) {
            client.send(SendMessage("Your inventory is full!"))
            return stopMiningInternal(client, MiningStopReason.FULL_INVENTORY)
        }

        if (rock.oreItemId != 1436) {
            client.send(SendMessage("You mine some ${client.GetItemName(rock.oreItemId).lowercase()}"))
        }
        client.addItem(rock.oreItemId, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, rock.oreItemId, 1, client.position.copy(), "Mining")
        client.giveExperience(rock.experience, Skill.MINING)
        client.triggerRandom(rock.experience)
        if (rock.randomGemEligible) {
            tryAwardRandomGem(client)
        }

        val cycle = GameCycleClock.currentCycle()
        val updatedState =
            state.copy(
                resourcesGathered = state.resourcesGathered + 1,
                nextSwingAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(SWING_REPEAT_DELAY_MS),
                nextResourceCycle = cycle + computeMiningDelayTicks(client, rock, pickaxe),
            )
        client.miningState = updatedState
        GameEventBus.post(MiningSuccessEvent(client, rock, rock.oreItemId, rock.experience, client.position.copy()))

        if (updatedState.resourcesGathered >= rock.restThreshold && Misc.chance(20) == 1) {
            client.send(SendMessage("You take a rest after gathering ${updatedState.resourcesGathered} resources."))
            return stopMiningInternal(client, MiningStopReason.RESTED)
        }

        client.requestAnim(pickaxe.animationId, 0)
        return true
    }

    @JvmStatic
    fun reapplyMiningAnimation(client: Client): Boolean {
        val state = client.miningState ?: return false
        val pickaxe =
            resolveBestPickaxe(client)
                ?: run {
                    client.send(SendMessage("You need a pickaxe in which you got the required mining level for."))
                    return stopMiningInternal(client, MiningStopReason.NO_PICKAXE)
                }

        client.requestAnim(pickaxe.animationId, 0)
        client.miningState =
            state.copy(nextSwingAnimationCycle = GameCycleClock.currentCycle() + GameCycleClock.ticksForDurationMs(SWING_REPEAT_DELAY_MS))
        return true
    }

    internal fun tryAwardRandomGem(client: Client): Int? {
        val chance = resolveRandomGemChance(client)
        return tryAwardRandomGem(client, chance, Misc.chance(chance), Misc.random(MiningDefinitions.randomGemDropTable.size - 1))
    }

    internal fun resolveRandomGemChance(client: Client): Int {
        return if (client.GetItemName(client.equipment[Equipment.Slot.NECK.id]).lowercase().contains("glory")) {
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
        val gem = MiningDefinitions.randomGemDropTable[gemIndex.coerceIn(0, MiningDefinitions.randomGemDropTable.lastIndex)]
        client.addItem(gem, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, gem, 1, client.position.copy(), "Mining")
        client.send(SendMessage("You found a ${client.GetItemName(gem).lowercase()} inside the rock."))
        return gem
    }

    private fun advanceMining(client: Client): Boolean {
        val state = client.miningState ?: return false
        MiningDefinitions.rockByObjectId[state.rockObjectId]
            ?: return stopMiningInternal(client, MiningStopReason.INVALID_ROCK)

        if (client.isBusy) {
            return stopMiningInternal(client, MiningStopReason.BUSY)
        }
        if (!isNearRock(client, state.rockPosition)) {
            return stopMiningInternal(client, MiningStopReason.MOVED_AWAY)
        }

        if (resolveBestPickaxe(client) == null) {
            client.send(SendMessage("You need a pickaxe in which you got the required mining level for."))
            return stopMiningInternal(client, MiningStopReason.NO_PICKAXE)
        }

        val cycle = GameCycleClock.currentCycle()
        if (cycle >= state.nextSwingAnimationCycle && !reapplyMiningAnimation(client)) {
            return false
        }
        if (cycle >= state.nextResourceCycle) {
            return performMiningCycle(client)
        }
        return true
    }

    private fun stopMiningInternal(
        client: Client,
        reason: MiningStopReason,
    ): Boolean {
        val state = client.miningState
        val rock = state?.let { MiningDefinitions.rockByObjectId[it.rockObjectId] }
        val position = state?.rockPosition?.copy()
        val hadMining = state != null || client.activeActionType == PlayerActionType.MINING

        client.clearMiningState()

        if (hadMining) {
            GameEventBus.post(MiningStoppedEvent(client, rock, position, reason))
        }
        return false
    }

    private fun mapStopReason(result: PlayerActionStopResult): MiningStopReason =
        when (result) {
            PlayerActionStopResult.Completed -> MiningStopReason.USER_INTERRUPT
            is PlayerActionStopResult.Cancelled ->
                when (result.reason) {
                    PlayerActionCancelReason.DISCONNECTED -> MiningStopReason.DISCONNECTED
                    PlayerActionCancelReason.MOVEMENT -> MiningStopReason.MOVED_AWAY
                    else -> MiningStopReason.USER_INTERRUPT
                }
        }

    private fun isNearRock(client: Client, rockPosition: Position): Boolean {
        if (client.position.z != rockPosition.z) {
            return false
        }
        val deltaX = kotlin.math.abs(client.position.x - rockPosition.x)
        val deltaY = kotlin.math.abs(client.position.y - rockPosition.y)
        return (deltaX + deltaY) == 1
    }

    private fun computeInitialMiningDelayTicks(
        client: Client,
        rock: MiningRockDef,
        pickaxe: PickaxeDef,
    ): Int {
        val remainingDelayMs = computeMiningDelayMs(client, rock, pickaxe) - INITIAL_SWING_DELAY_MS
        return if (remainingDelayMs <= 0L) 0 else GameCycleClock.ticksForDurationMs(remainingDelayMs)
    }

}
