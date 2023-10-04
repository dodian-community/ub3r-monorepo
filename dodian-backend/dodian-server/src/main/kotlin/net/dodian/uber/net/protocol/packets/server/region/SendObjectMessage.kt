package net.dodian.uber.net.protocol.packets.server.region

import net.dodian.uber.game.modelkt.entity.`object`.GameObject

data class SendObjectMessage(
    val id: Int,
    val orientation: Int,
    val offset: Int,
    val type: Int,
    override val priority: Int = LOW_PRIORITY
) : RegionUpdateMessage() {

    constructor(gameObj: GameObject, offset: Int) : this(gameObj.id, gameObj.orientation, offset, gameObj.type)
}