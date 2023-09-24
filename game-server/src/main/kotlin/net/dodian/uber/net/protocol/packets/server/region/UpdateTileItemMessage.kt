package net.dodian.uber.net.protocol.packets.server.region

import net.dodian.uber.game.modelkt.Item

data class UpdateTileItemMessage(
    val item: Item,
    val previousAmount: Int,
    val offset: Int = 0,
    override val priority: Int = LOW_PRIORITY
) : RegionUpdateMessage()