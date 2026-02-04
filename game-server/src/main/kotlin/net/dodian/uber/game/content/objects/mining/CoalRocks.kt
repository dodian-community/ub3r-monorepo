package net.dodian.uber.game.content.objects.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object CoalRocks : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        7456,
        7489,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val rockId = Utils.rocks.indexOf(objectId)
        if (rockId == -1) return false

        if (client.fletchings || client.isFiremaking || client.shafting) {
            client.resetAction()
        }
        if (client.getPositionName(client.position) == Player.positions.TZHAAR) {
            client.send(SendMessage("You can not mine here or the Tzhaar's will be angry!"))
            return true
        }
        if (client.getLevel(Skill.MINING) < Utils.rockLevels[rockId]) {
            client.send(SendMessage("You need a mining level of ${Utils.rockLevels[rockId]} to mine this rock"))
            return true
        }
        val minePick = client.findPick()
        if (minePick == -1) {
            client.resetAction()
            client.send(SendMessage("You need a pickaxe in which you got the required mining level for."))
            return true
        }

        client.resourcesGathered = 0
        client.lastPickAction = System.currentTimeMillis() + 600
        client.lastAction = System.currentTimeMillis() - 600
        client.mineIndex = rockId
        client.mining = true
        client.requestAnim(client.getMiningEmote(Utils.picks[minePick]), 0)
        client.send(SendMessage("You swing your pick at the rock..."))
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.send(SendMessage("This rock contains coal."))
        return true
    }
}
