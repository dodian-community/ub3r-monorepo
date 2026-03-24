package net.dodian.uber.game.skills.fletching

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.runtime.action.SkillingActionService

object FletchingService {
    @JvmStatic
    fun openBowSelection(client: Client, logIndex: Int) {
        client.resetAction()
        client.dialogInterface = 2459
        client.fletchingState = FletchingState(logIndex = logIndex)
        client.send(SendString("Select a bow", 8879))
        client.sendFrame246(8870, 250, Constants.longbows[logIndex])
        client.sendFrame246(8869, 250, Constants.shortbows[logIndex])
        client.send(SendString(client.GetItemName(Constants.shortbows[logIndex]), 8871))
        client.send(SendString(client.GetItemName(Constants.shortbows[logIndex]), 8874))
        client.send(SendString(client.GetItemName(Constants.longbows[logIndex]), 8878))
        client.send(SendString(client.GetItemName(Constants.longbows[logIndex]), 8875))
        client.sendFrame164(8866)
    }

    @JvmStatic
    fun startBowCrafting(client: Client, longBow: Boolean, amount: Int) {
        client.send(RemoveInterfaces())
        val logIndex = client.fletchingState?.logIndex ?: -1
        if (logIndex !in Constants.logs.indices) {
            client.resetAction()
            return
        }

        val request =
        if (longBow) {
            if (client.getLevel(Skill.FLETCHING) < Constants.longreq[logIndex]) {
                client.send(SendMessage("Requires fletching ${Constants.longreq[logIndex]}!"))
                client.resetAction()
                return
            }
            FletchingRequest(logIndex, Constants.longbows[logIndex], Constants.longexp[logIndex], amount)
        } else {
            if (client.getLevel(Skill.FLETCHING) < Constants.shortreq[logIndex]) {
                client.send(SendMessage("Requires fletching ${Constants.shortreq[logIndex]}!"))
                client.resetAction()
                return
            }
            FletchingRequest(logIndex, Constants.shortbows[logIndex], Constants.shortexp[logIndex], amount)
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
            client.send(SendMessage("You are currently busy to be fletching!"))
            return
        }

        client.send(RemoveInterfaces())
        client.IsBanking = false
        client.requestAnim(4433, 0)

        val logIndex = state.logIndex
        if (logIndex !in Constants.logs.indices || !client.playerHasItem(Constants.logs[logIndex])) {
            client.resetAction()
            return
        }

        client.deleteItem(Constants.logs[logIndex], 1)
        client.addItem(state.productId, 1)
        client.checkItemUpdate()
        client.giveExperience(state.experience, Skill.FLETCHING)
        client.triggerRandom(state.experience)
        client.fletchingState = state.copy(remaining = state.remaining - 1)
    }
}
