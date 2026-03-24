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
        client.fletchLog = logIndex
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
        val logIndex = client.fletchLog
        if (logIndex !in Constants.logs.indices) {
            client.resetAction()
            return
        }

        if (longBow) {
            if (client.getLevel(Skill.FLETCHING) < Constants.longreq[logIndex]) {
                client.send(SendMessage("Requires fletching ${Constants.longreq[logIndex]}!"))
                client.resetAction()
                return
            }
            client.fletchId = Constants.longbows[logIndex]
            client.fletchExp = Constants.longexp[logIndex]
        } else {
            if (client.getLevel(Skill.FLETCHING) < Constants.shortreq[logIndex]) {
                client.send(SendMessage("Requires fletching ${Constants.shortreq[logIndex]}!"))
                client.resetAction()
                return
            }
            client.fletchId = Constants.shortbows[logIndex]
            client.fletchExp = Constants.shortexp[logIndex]
        }

        client.fletchAmount = amount
        client.fletchings = true
        SkillingActionService.startFletching(client)
    }

    @JvmStatic
    fun performBowCycle(client: Client) {
        if (client.fletchAmount < 1) {
            client.resetAction()
            return
        }
        if (client.isBusy) {
            client.send(SendMessage("You are currently busy to be fletching!"))
            return
        }

        client.fletchAmount--
        client.send(RemoveInterfaces())
        client.IsBanking = false
        client.requestAnim(4433, 0)

        val logIndex = client.fletchLog
        if (logIndex !in Constants.logs.indices || !client.playerHasItem(Constants.logs[logIndex])) {
            client.resetAction()
            return
        }

        client.deleteItem(Constants.logs[logIndex], 1)
        client.addItem(client.fletchId, 1)
        client.checkItemUpdate()
        client.giveExperience(client.fletchExp, Skill.FLETCHING)
        client.triggerRandom(client.fletchExp)
    }
}
