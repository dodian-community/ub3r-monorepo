package net.dodian.uber.net.protocol.packets.server.regionupdate

import net.dodian.uber.game.modelkt.Item

data class RemoveTileItemMessage(
    val id: Int,
    val offset: Int,
    override val priority: Int = HIGH_PRIORITY
) : RegionUpdateMessage() {
    constructor(item: Item, offset: Int) : this(item.id, offset)
}