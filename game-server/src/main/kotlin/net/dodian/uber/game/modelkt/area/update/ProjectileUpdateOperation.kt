package net.dodian.uber.game.modelkt.area.update

import net.dodian.uber.game.modelkt.area.EntityUpdateType
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.entity.Projectile
import net.dodian.uber.net.protocol.packets.server.regionupdate.RegionUpdateMessage
import net.dodian.uber.net.protocol.packets.server.regionupdate.SendProjectileMessage

class ProjectileUpdateOperation(
    region: Region,
    updateType: EntityUpdateType,
    entity: Projectile
) : UpdateOperation<Projectile>(entity, region, updateType) {

    override fun remove(offset: Int): RegionUpdateMessage {
        error("Projectiles cannot be removed.")
    }

    override fun add(offset: Int) = SendProjectileMessage(entity, offset)
}