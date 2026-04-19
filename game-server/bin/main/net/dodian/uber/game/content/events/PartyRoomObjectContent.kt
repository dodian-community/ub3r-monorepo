package net.dodian.uber.game.content.events

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.events.partyroom.Balloons

object PartyRoomObjectContent : ObjectContent {
    override val objectIds: IntArray = PartyRoomObjectIds.allObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId in PartyRoomObjectIds.balloonObjects && Balloons.lootBalloon(client, position.copy())) {
            return true
        }
        if (objectId == PartyRoomObjectIds.DEPOSIT_CHEST) {
            Balloons.openInterface(client)
            return true
        }
        if (objectId == PartyRoomObjectIds.FORCE_TRIGGER && client.playerRights > 1) {
            Balloons.triggerPartyEvent(client)
            return true
        }
        return false
    }
}
