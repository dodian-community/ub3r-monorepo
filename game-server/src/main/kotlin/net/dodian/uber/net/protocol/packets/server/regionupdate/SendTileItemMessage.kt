package net.dodian.uber.net.protocol.packets.server.regionupdate

import net.dodian.uber.game.modelkt.Item

data class SendTileItemMessage(
    val item: Item,
    val offset: Int,
    override val priority: Int = LOW_PRIORITY
) : RegionUpdateMessage()