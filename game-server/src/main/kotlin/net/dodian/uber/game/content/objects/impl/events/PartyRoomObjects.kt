package net.dodian.uber.game.content.objects.impl.events

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.party.Balloons

object PartyRoomObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        115, 116, 117, 118, 119, 120, 121, 122,
        26193, 26194,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId in 115..122 && Balloons.lootBalloon(client, position.copy())) {
            return true
        }
        if (objectId == 26193) {
            Balloons.openInterface(client)
            return true
        }
        if (objectId == 26194 && client.playerRights > 1) {
            Balloons.triggerPartyEvent(client)
            return true
        }
        return false
    }
}
