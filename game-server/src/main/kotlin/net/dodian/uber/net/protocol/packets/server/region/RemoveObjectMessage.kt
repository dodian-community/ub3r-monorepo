package net.dodian.uber.net.protocol.packets.server.region

import net.dodian.uber.game.modelkt.entity.`object`.GameObject

data class RemoveObjectMessage(
    val orientation: Int,
    val offset: Int,
    val type: Int,
    override val priority: Int = HIGH_PRIORITY
) : RegionUpdateMessage() {

    constructor(gameObject: GameObject, offset: Int) : this(
        offset = offset,
        type = gameObject.type,
        orientation = gameObject.orientation
    )
}