package net.dodian.uber.net.protocol.packets.server.regionupdate

import net.dodian.uber.game.modelkt.entity.Projectile

data class SendProjectileMessage(
    val projectile: Projectile,
    val offset: Int,
    override val priority: Int = LOW_PRIORITY
) : RegionUpdateMessage()