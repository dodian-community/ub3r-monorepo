package net.dodian.uber.game.content.skills.fletching

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.systems.action.SkillingActionService

object FletchingService {
    @JvmStatic
    fun openBowSelection(client: Client, logIndex: Int) {
        client.resetAction()
        client.dialogInterface = 2459
        client.fletchingState = FletchingState(logIndex = logIndex)
        val bowLog = FletchingDefinitions.bowLog(logIndex) ?: return
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
        val bowLog = FletchingDefinitions.bowLog(logIndex)
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
        SkillingActionService.startFletching(client)
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
        val bowLog = FletchingDefinitions.bowLog(logIndex)
        if (bowLog == null || !client.playerHasItem(bowLog.logItemId)) {
            client.resetAction()
            return
        }

        client.deleteItem(bowLog.logItemId, 1)
        client.addItem(state.productId, 1)
        client.checkItemUpdate()
        SkillProgressionService.gainXp(client, state.experience, Skill.FLETCHING)
        SkillingRandomEventService.trigger(client, state.experience)
        client.fletchingState = state.copy(remaining = state.remaining - 1)
    }
}
