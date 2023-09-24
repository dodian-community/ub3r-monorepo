package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.RegionCoordinates
import net.dodian.uber.net.message.Message

data class ClearRegionMessage(
    val player: Position,
    val region: Position
) : Message() {
    
    constructor(player: Position, region: RegionCoordinates)
            : this(player, Position(region.absoluteX, region.absoluteY))
}