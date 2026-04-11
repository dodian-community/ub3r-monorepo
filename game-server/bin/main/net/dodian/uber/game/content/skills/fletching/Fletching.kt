package net.dodian.uber.game.content.skills.fletching

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.content.skills.runtime.action.ActionStopReason
import net.dodian.uber.game.content.skills.runtime.action.RunningProductionAction
import net.dodian.uber.game.content.skills.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.content.skills.runtime.action.productionAction
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.systems.action.PolicyPreset
import net.dodian.uber.game.systems.skills.plugin.SkillPlugin
import net.dodian.uber.game.systems.skills.plugin.skillPlugin
import java.util.Collections
import java.util.WeakHashMap

object Fletching {
    private const val STANDARD_ACTION_DELAY_MS = 1800L
    private val fletchingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningProductionAction>())

    @JvmStatic
    fun open(client: Client, logIndex: Int) = openBowSelection(client, logIndex)

    @JvmStatic
    fun start(client: Client, longBow: Boolean, amount: Int) = startBowCrafting(client, longBow, amount)

    @JvmStatic
    fun openBowSelection(client: Client, logIndex: Int) {
        client.resetAction()
        client.dialogInterface = 2459
        client.fletchingState = FletchingState(logIndex = logIndex)
        val bowLog = FletchingData.bowLog(logIndex) ?: return
        client.sendString("Select a bow", 8879)
        client.sendInterfaceModel(8870, 250, bowLog.unstrungLongbowId)
        client.sendInterfaceModel(8869, 250, bowLog.unstrungShortbowId)
        client.sendString(client.getItemName(bowLog.unstrungShortbowId), 8871)
        client.sendString(client.getItemName(bowLog.unstrungShortbowId), 8874)
        client.sendString(client.getItemName(bowLog.unstrungLongbowId), 8878)
        client.sendString(client.getItemName(bowLog.unstrungLongbowId), 8875)
        client.sendChatboxInterface(8866)
    }

    @JvmStatic
    fun startBowCrafting(client: Client, longBow: Boolean, amount: Int) {
        client.send(RemoveInterfaces())
        val logIndex = client.fletchingState?.logIndex ?: -1
        val bowLog = FletchingData.bowLog(logIndex)
        if (bowLog == null) {
            client.resetAction()
            return
        }

        val request =
        if (longBow) {
            if (client.getLevel(Skill.FLETCHING) < bowLog.longLevelRequired) {
                client.sendMessage("Requires fletching ${bowLog.longLevelRequired}!")
                client.resetAction()
                return
            }
            FletchingRequest(logIndex, bowLog.unstrungLongbowId, bowLog.longExperience, amount)
        } else {
            if (client.getLevel(Skill.FLETCHING) < bowLog.shortLevelRequired) {
                client.sendMessage("Requires fletching ${bowLog.shortLevelRequired}!")
                client.resetAction()
                return
            }
            FletchingRequest(logIndex, bowLog.unstrungShortbowId, bowLog.shortExperience, amount)
        }

        start(client, request)
    }

    @JvmStatic
    fun start(client: Client, request: FletchingRequest) {
        client.fletchingState =
            FletchingState(
                logIndex = request.logIndex,
                productId = request.productId,
                experience = request.experience,
                remaining = request.amount,
            )
        startAction(client)
    }

    @JvmStatic
    fun startAction(client: Client) {
        stopAction(client, ActionStopReason.USER_INTERRUPT)
        val action =
            productionAction("fletching") {
                delay { GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS) }
                onCycleWhile {
                    if ((fletchingState?.remaining ?: 0) <= 0) {
                        return@onCycleWhile false
                    }
                    performBowCycle(this)
                    (fletchingState?.remaining ?: 0) > 0
                }
                onStop {
                    fletchingTasks.remove(this)
                    clearFletchingState()
                }
            }
        val running = action.start(client) ?: run {
            client.clearFletchingState()
            return
        }
        fletchingTasks[client] = running
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun stopFromReset(client: Client, fullReset: Boolean) {
        stopAction(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopAction(
        client: Client,
        reason: ActionStopReason = ActionStopReason.USER_INTERRUPT,
    ) {
        fletchingTasks.remove(client)?.cancel(reason)
    }

    @JvmStatic
    fun performBowCycle(client: Client) {
        val state = client.fletchingState ?: run {
            client.resetAction()
            return
        }
        if (state.remaining < 1) {
            client.resetAction()
            return
        }
        if (client.isBusy) {
            client.sendMessage("You are currently busy to be fletching!")
            return
        }

        client.send(RemoveInterfaces())
        client.IsBanking = false
        client.performAnimation(4433, 0)

        val logIndex = state.logIndex
        val bowLog = FletchingData.bowLog(logIndex)
        if (bowLog == null || !client.playerHasItem(bowLog.logItemId)) {
            client.resetAction()
            return
        }

        client.deleteItem(bowLog.logItemId, 1)
        client.addItem(state.productId, 1)
        client.checkItemUpdate()
        ProgressionService.addXp(client, state.experience, Skill.FLETCHING)
        SkillingRandomEventService.trigger(client, state.experience)
        client.fletchingState = state.copy(remaining = state.remaining - 1)
    }
}

object FletchingSkillPlugin : SkillPlugin {
    private val knifeIds = intArrayOf(946, 5605)

    override val definition =
        skillPlugin(name = "Fletching", skill = Skill.FLETCHING) {
            val logIds = FletchingData.bowLogs.map { it.logItemId }.distinct()
            for (knifeId in knifeIds) {
                for (logId in logIds) {
                    itemOnItem(preset = PolicyPreset.PRODUCTION, leftItemId = knifeId, rightItemId = logId) { client, itemUsed, otherItem ->
                        val usedLogId = if (itemUsed == knifeId) otherItem else itemUsed
                        val logIndex = FletchingData.bowLogs.indexOfFirst { it.logItemId == usedLogId }
                        if (logIndex < 0) {
                            false
                        } else {
                            Fletching.open(client, logIndex)
                            true
                        }
                    }
                }
            }
        }
}
