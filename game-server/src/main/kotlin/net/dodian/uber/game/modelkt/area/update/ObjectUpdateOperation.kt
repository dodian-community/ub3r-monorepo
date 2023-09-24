package net.dodian.uber.game.modelkt.area.update

import net.dodian.uber.game.modelkt.area.EntityUpdateType
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.entity.`object`.GameObject
import net.dodian.uber.net.protocol.packets.server.regionupdate.RemoveObjectMessage
import net.dodian.uber.net.protocol.packets.server.regionupdate.SendObjectMessage

class ObjectUpdateOperation(
    entity: GameObject,
    region: Region,
    updateType: EntityUpdateType
) : UpdateOperation<GameObject>(entity, region, updateType) {

    override fun remove(offset: Int) = RemoveObjectMessage(entity, offset)
    override fun add(offset: Int) = SendObjectMessage(entity, offset)
}