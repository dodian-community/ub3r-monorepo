package net.dodian.uber.net.protocol.packets.server.region

import net.dodian.uber.game.modelkt.Item

data class SendPublicTileItemMessage(
    val index: Int,
    val item: Item,
    val offset: Int,
    override val priority: Int = LOW_PRIORITY
) : RegionUpdateMessage()