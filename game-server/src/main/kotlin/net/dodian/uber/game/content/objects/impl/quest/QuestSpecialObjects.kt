package net.dodian.uber.game.content.objects.impl.quest

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object QuestSpecialObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(7962, 20931, 25824, 14889)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            7962 -> {
                client.send(SendMessage("You inspect the monolith, but can't make sense of the inscription."))
                true
            }
            20931 -> {
                client.getPlunder.resetPlunder()
                true
            }
            25824, 14889 -> {
                client.spinning = true
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                true
            }
            else -> false
        }
    }
}
